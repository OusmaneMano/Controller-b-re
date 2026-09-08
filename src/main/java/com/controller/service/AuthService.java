package com.controller.service;

import com.controller.dto.AuthDTO;
import com.controller.entity.User;
import com.controller.entity.enums.UserRole;
import com.controller.entity.enums.UserStatus;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // ==================== MANAGER AUTHENTICATION ====================

    /**
     * Manager signup using generic SignupRequest
     */
    public AuthDTO.AuthResponse managerSignup(AuthDTO.SignupRequest request) {
        log.info("Manager signup attempt: {}", request.getEmail());

        // Validate email not already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already registered: {}", request.getEmail());
            throw new IllegalArgumentException("Email already registered");
        }

        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .country(request.getCountry())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.MANAGER)
                .userStatus(UserStatus.PENDING_APPROVAL)
                .termsAccepted(request.getAcceptedTerms() != null && request.getAcceptedTerms())
                .termsAcceptedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Manager account created: {}", savedUser.getId());

        return AuthDTO.AuthResponse.builder()
                .token(null) // Token generation deferred until approval
                .user(AuthDTO.UserInfo.builder()
                        .id(savedUser.getId())
                        .email(savedUser.getEmail())
                        .firstName(savedUser.getFirstName())
                        .lastName(savedUser.getLastName())
                        .role(savedUser.getRole().toString())
                        .build())
                .message("Manager account created. Awaiting payment approval.")
                .build();
    }

    /**
     * Manager login using generic LoginRequest
     */
    public AuthDTO.AuthResponse managerLogin(AuthDTO.LoginRequest request) {
        log.info("Manager login attempt: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (user.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException("Not a manager account");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Account not active. Awaiting approval.");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().toString());
        log.info("Manager logged in successfully: {}", user.getEmail());

        return AuthDTO.AuthResponse.builder()
                .token(token)
                .user(AuthDTO.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().toString())
                        .build())
                .build();
    }

    // ==================== EMPLOYEE AUTHENTICATION ====================

    /**
     * Employee signup using generic SignupRequest
     */
    public AuthDTO.AuthResponse employeeSignup(AuthDTO.SignupRequest request) {
        log.info("Employee signup attempt: {}", request.getEmail());

        // Validate email not already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already registered: {}", request.getEmail());
            throw new IllegalArgumentException("Email already registered");
        }

        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Create employee
        User employee = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.EMPLOYEE)
                .userStatus(UserStatus.ACTIVE)
                .termsAccepted(request.getAcceptedTerms() != null && request.getAcceptedTerms())
                .termsAcceptedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        User savedEmployee = userRepository.save(employee);
        log.info("Employee account created: {}", savedEmployee.getId());

        return AuthDTO.AuthResponse.builder()
                .user(AuthDTO.UserInfo.builder()
                        .id(savedEmployee.getId())
                        .email(savedEmployee.getEmail())
                        .firstName(savedEmployee.getFirstName())
                        .lastName(savedEmployee.getLastName())
                        .role(savedEmployee.getRole().toString())
                        .build())
                .message("Employee account created successfully")
                .build();
    }

    /**
     * Employee login using generic LoginRequest
     */
    public AuthDTO.AuthResponse employeeLogin(AuthDTO.LoginRequest request) {
        log.info("Employee login attempt: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (user.getRole() != UserRole.EMPLOYEE) {
            throw new IllegalArgumentException("Not an employee account");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Account not active");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().toString());
        log.info("Employee logged in successfully: {}", user.getEmail());

        return AuthDTO.AuthResponse.builder()
                .token(token)
                .user(AuthDTO.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().toString())
                        .build())
                .build();
    }

    /**
     * Employee credential verification using company ID and credentials
     */
    public AuthDTO.AuthResponse verifyEmployeeCredentials(Long companyId, AuthDTO.EmployeeCredentialsRequest request) {
        log.info("Employee credential verification for company {}: {}", companyId, request.getEmployeeEmail());

        // Verify employee exists
        User employee = userRepository.findByEmail(request.getEmployeeEmail())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (employee.getRole() != UserRole.EMPLOYEE) {
            throw new IllegalArgumentException("User is not an employee");
        }

        // In Phase 2, properly verify manager credentials and company association
        // For MVP, we'll just create a token for the employee
        
        String token = jwtTokenProvider.generateToken(
                request.getEmployeeEmail(),
                UserRole.EMPLOYEE.toString()
        );

        log.info("Employee credentials verified: {}", request.getEmployeeEmail());

        return AuthDTO.AuthResponse.builder()
                .token(token)
                .user(AuthDTO.UserInfo.builder()
                        .id(employee.getId())
                        .email(employee.getEmail())
                        .firstName(employee.getFirstName())
                        .lastName(employee.getLastName())
                        .role(employee.getRole().toString())
                        .build())
                .message("Employee credentials verified")
                .build();
    }

    // ==================== GOOGLE AUTHENTICATION ====================

    /**
     * Google OAuth login
     */
    public AuthDTO.AuthResponse googleLogin(AuthDTO.GoogleLoginRequest request) {
        log.info("Google login attempt with ID token");

        // TODO: Implement Google OAuth verification
        // 1. Verify the ID token with Google's API
        // 2. Extract user email from token
        // 3. Find or create user in database
        // 4. Generate JWT token
        
        throw new UnsupportedOperationException("Google OAuth login not yet implemented");
    }

    // ==================== ADMIN AUTHENTICATION ====================

    /**
     * Admin login using generic LoginRequest
     */
    public AuthDTO.AuthResponse adminLogin(AuthDTO.LoginRequest request) {
        log.info("Admin login attempt: {}", request.getEmail());

        // For MVP, simple hardcoded admin check
        if ("admin@controller.com".equals(request.getEmail()) && 
            "admin123".equals(request.getPassword())) {
            
            String token = jwtTokenProvider.generateToken(
                    request.getEmail(),
                    UserRole.ADMIN.toString()
            );
            
            log.info("Admin logged in: {}", request.getEmail());
            
            return AuthDTO.AuthResponse.builder()
                    .token(token)
                    .user(AuthDTO.UserInfo.builder()
                            .id(1L)
                            .email(request.getEmail())
                            .role(UserRole.ADMIN.toString())
                            .build())
                    .build();
        }

        throw new IllegalArgumentException("Invalid admin credentials");
    }

    // ==================== TOKEN REFRESH ====================

    /**
     * Refresh authentication token
     */
    public AuthDTO.AuthResponse refreshToken(AuthDTO.RefreshTokenRequest request) {
        log.info("Refresh token request");
        
        // TODO: Implement token refresh logic
        // 1. Validate refresh token
        // 2. Extract user from refresh token
        // 3. Generate new access token
        // 4. Return new token
        
        throw new UnsupportedOperationException("Token refresh not yet implemented");
    }
}
