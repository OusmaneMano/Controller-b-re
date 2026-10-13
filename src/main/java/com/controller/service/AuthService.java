package com.controller.service;

import com.controller.dto.AuthDTO;
import com.controller.entity.Company;
import com.controller.entity.User;
import com.controller.entity.enums.UserRole;
import com.controller.entity.enums.UserStatus;
import com.controller.repository.CompanyRepository;
import com.controller.repository.UserRepository;
import com.controller.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MembershipService membershipService;
    private final DemoCompanyService demoCompanyService;

    public AuthDTO.AuthResponse managerSignup(AuthDTO.SignupRequest request) {
        validateNewUser(request);
        User user = persistUser(request, UserRole.MANAGER, UserStatus.EXPLORING);
        Company demo = demoCompanyService.getOrCreateDemoCompany();
        membershipService.add(user, demo, UserRole.EMPLOYEE);
        user.setLastCompanyId(demo.getId());
        user.setCompany(demo);
        userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getEmail(), UserRole.MANAGER.toString());
        return buildAuth(token, user, demo, UserRole.EMPLOYEE.toString(),
                "Account created. Explore the demo shop — when you want your own table, mark payment sent.");
    }

    public AuthDTO.AuthResponse managerLogin(AuthDTO.LoginRequest request) {
        User user = loadByEmail(request.getEmail());
        if (user.getRole() == UserRole.EMPLOYEE) {
            throw new IllegalArgumentException("Not a manager account — use employee login, or join a company.");
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Use the admin login endpoint");
        }
        checkPassword(request.getPassword(), user);
        if (user.getUserStatus() == UserStatus.SUSPENDED || user.getUserStatus() == UserStatus.DELETED) {
            throw new IllegalArgumentException("Account not active");
        }
        user.setLastLogin(LocalDateTime.now());
        demoCompanyService.ensureDemoMembership(user);
        userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getEmail(), UserRole.MANAGER.toString());
        Company current = currentCompany(user);
        String role = currentRole(user, current);
        return buildAuth(token, user, current, role, null);
    }

    public AuthDTO.AuthResponse employeeSignup(AuthDTO.SignupRequest request) {
        validateNewUser(request);
        User user = persistUser(request, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        return AuthDTO.AuthResponse.builder()
                .user(toUserInfo(user))
                .memberships(membershipService.listFor(user))
                .message("Employee account created. Log in, then join a company with the shared username and PIN.")
                .build();
    }

    public AuthDTO.AuthResponse employeeLogin(AuthDTO.LoginRequest request) {
        User user = loadByEmail(request.getEmail());
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Use the admin login endpoint");
        }
        checkPassword(request.getPassword(), user);
        if (user.getUserStatus() == UserStatus.SUSPENDED || user.getUserStatus() == UserStatus.DELETED) {
            throw new IllegalArgumentException("Account not active");
        }
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().toString());
        Company current = currentCompany(user);
        String role = currentRole(user, current);
        return buildAuth(token, user, current, role, null);
    }

    public AuthDTO.AuthResponse verifyEmployeeCredentials(AuthDTO.EmployeeCredentialsRequest request) {
        User user = loadByEmail(request.getEmployeeEmail());
        Company company = companyRepository.findByEmployeeUsername(request.getManagerUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!company.getEmployeePassword().equals(request.getManagerPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        membershipService.add(user, company, UserRole.EMPLOYEE);
        user.setLastCompanyId(company.getId());
        user.setCompany(company);
        userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().toString());
        return buildAuth(token, user, company, UserRole.EMPLOYEE.toString(),
                "You're now connected to " + company.getName() + ". Switch companies anytime.");
    }

    public AuthDTO.AuthResponse requestPayment(String email, AuthDTO.PaymentRequest request) {
        User user = loadByEmail(email);
        if (user.getUserStatus() == UserStatus.ACTIVE) {
            return buildAuth(null, user, currentCompany(user), currentRole(user, currentCompany(user)),
                    "Already approved — you can set up your company.");
        }
        user.setPaymentNote(request != null ? request.getNote() : null);
        user.setPaymentRequestedAt(LocalDateTime.now());
        user.setUserStatus(UserStatus.PAYMENT_SENT);
        userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().toString());
        return buildAuth(token, user, currentCompany(user), currentRole(user, currentCompany(user)),
                "Payment marked as sent. Keep exploring the demo while you wait for approval.");
    }

    public AuthDTO.AuthResponse switchCompany(String email, Long companyId) {
        var m = membershipService.require(email, companyId);
        User user = m.getUser();
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().toString());
        return buildAuth(token, user, m.getCompany(), m.getRole().toString(),
                "Switched to " + m.getCompany().getName());
    }

    public AuthDTO.AuthResponse me(String email) {
        User user = loadByEmail(email);
        Company current = currentCompany(user);
        return buildAuth(null, user, current, currentRole(user, current), null);
    }

    public AuthDTO.AuthResponse googleLogin(AuthDTO.GoogleLoginRequest request) {
        if (request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new IllegalArgumentException("Google token is required");
        }
        String email = verifyGoogleToken(request.getIdToken());
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Invalid Google account");
        }
        email = email.toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Auto-create as exploring manager from verified Google account
            user = User.builder()
                    .email(email)
                    .firstName(lastGoogleGiven != null ? lastGoogleGiven : "Google")
                    .lastName(lastGoogleFamily != null ? lastGoogleFamily : "User")
                    .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                    .role(UserRole.MANAGER)
                    .userStatus(UserStatus.EXPLORING)
                    .termsAccepted(true)
                    .termsAcceptedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
            user = userRepository.save(user);
            Company demo = demoCompanyService.getOrCreateDemoCompany();
            membershipService.add(user, demo, UserRole.EMPLOYEE);
            user.setLastCompanyId(demo.getId());
            user.setCompany(demo);
            userRepository.save(user);
        }
        if (user.getUserStatus() == UserStatus.SUSPENDED) {
            throw new IllegalArgumentException("Account blocked");
        }
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().toString());
        Company company = user.getCompany();
        return buildAuth(token, user, company, user.getRole().toString(), "Signed in with Google");
    }

        private String verifyGoogleToken(String token) {
        try {
            // Try as id_token first
            String email = fetchEmail("https://oauth2.googleapis.com/tokeninfo?id_token=" + java.net.URLEncoder.encode(token, "UTF-8"), null);
            if (email != null) return email;
            // Fallback: treat as access token
            return fetchEmail("https://www.googleapis.com/oauth2/v3/userinfo", token);
        } catch (Exception e) {
            log.warn("Google token verify failed: {}", e.getMessage());
            return null;
        }
    }

    private String fetchEmail(String url, String bearer) throws Exception {
        java.net.http.HttpRequest.Builder b = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).GET();
        if (bearer != null) b.header("Authorization", "Bearer " + bearer);
        java.net.http.HttpResponse<String> res = java.net.http.HttpClient.newHttpClient()
                .send(b.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            log.warn("Google verify status {} for {}", res.statusCode(), url);
            return null;
        }
        String body = res.body();
        String email = extractJson(body, "email");
        if (email == null) return null;
        lastGoogleGiven = extractJson(body, "given_name");
        lastGoogleFamily = extractJson(body, "family_name");
        return email;
    }

    private String lastGoogleGiven;
    private String lastGoogleFamily;

    private String extractJson(String body, String key) {
        String search = "\"" + key + "\"";
        int i = body.indexOf(search);
        if (i < 0) return null;
        int start = body.indexOf('"', i + search.length()) + 1;
        int end = body.indexOf('"', start);
        if (start <= 0 || end <= start) return null;
        return body.substring(start, end);
    }

    public AuthDTO.AuthResponse adminLogin(AuthDTO.LoginRequest request) {
        if ("admin@controller.com".equals(request.getEmail()) && "admin123".equals(request.getPassword())) {
            String token = jwtTokenProvider.generateToken(request.getEmail(), UserRole.ADMIN.toString());
            return AuthDTO.AuthResponse.builder()
                    .token(token)
                    .user(AuthDTO.UserInfo.builder()
                            .id(1L)
                            .email(request.getEmail())
                            .role(UserRole.ADMIN.toString())
                            .status(UserStatus.ACTIVE.toString())
                            .build())
                    .build();
        }
        throw new IllegalArgumentException("Invalid admin credentials");
    }

    public AuthDTO.AuthResponse refreshToken(AuthDTO.RefreshTokenRequest request) {
        throw new UnsupportedOperationException("Token refresh not yet implemented");
    }

    private void validateNewUser(AuthDTO.SignupRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        String email = request.getEmail().toLowerCase().trim();
        if (isDisposable(email)) {
            throw new IllegalArgumentException("Please use a real email address. Temporary emails are not allowed.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
    }

    private User persistUser(AuthDTO.SignupRequest request, UserRole role, UserStatus status) {
        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .country(request.getCountry())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .userStatus(status)
                .termsAccepted(request.getAcceptedTerms() != null && request.getAcceptedTerms())
                .termsAcceptedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private User loadByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
    }

    private void checkPassword(String raw, User user) {
        if (!passwordEncoder.matches(raw, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
    }

    private Company currentCompany(User user) {
        if (user.getLastCompanyId() != null) {
            return companyRepository.findById(user.getLastCompanyId()).orElse(user.getCompany());
        }
        return user.getCompany();
    }

    private String currentRole(User user, Company company) {
        if (company == null) return user.getRole().toString();
        return membershipService.listFor(user).stream()
                .filter(m -> company.getId().equals(m.getCompanyId()))
                .map(AuthDTO.MembershipInfo::getRole)
                .findFirst()
                .orElse(user.getRole().toString());
    }

    private AuthDTO.UserInfo toUserInfo(User user) {
        return AuthDTO.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().toString())
                .status(user.getUserStatus() != null ? user.getUserStatus().toString() : null)
                .paymentNote(user.getPaymentNote())
                .build();
    }

    private AuthDTO.AuthResponse buildAuth(String token, User user, Company company, String roleInCompany, String message) {
        AuthDTO.CompanyInfo info = null;
        if (company != null) {
            info = AuthDTO.CompanyInfo.builder()
                    .id(company.getId())
                    .name(company.getName())
                    .status(company.getStatus() != null ? company.getStatus().toString() : null)
                    .demo(Boolean.TRUE.equals(company.getDemo()))
                    .roleInCompany(roleInCompany)
                    .build();
        }
        return AuthDTO.AuthResponse.builder()
                .token(token)
                .user(toUserInfo(user))
                .company(info)
                .memberships(membershipService.listFor(user))
                .message(message)
                .build();
    }

    private boolean isDisposable(String email) {
        String domain = email.substring(email.indexOf('@') + 1);
        java.util.Set<String> blocked = java.util.Set.of(
            "tempmail.com", "10minutemail.com", "guerrillamail.com", "mailinator.com",
            "yopmail.com", "throwaway.email", "temp-mail.org", "getnada.com",
            "trashmail.com", "fakeinbox.com", "sharklasers.com", "guerrillamailblock.com"
        );
        return blocked.contains(domain);
    }
}
