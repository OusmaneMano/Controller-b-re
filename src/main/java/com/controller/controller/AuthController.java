package com.controller.controller;

import com.controller.dto.AuthDTO;
import com.controller.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    // ==================== MANAGER ENDPOINTS ====================

    /**
     * Manager signup endpoint
     * POST /api/auth/manager/signup
     */
    @PostMapping("/manager/signup")
    public ResponseEntity<AuthDTO.AuthResponse> managerSignup(@RequestBody AuthDTO.SignupRequest request) {
        log.info("Received manager signup request for email: {}", request.getEmail());
        AuthDTO.AuthResponse response = authService.managerSignup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Manager login endpoint
     * POST /api/auth/manager/login
     */
    @PostMapping("/manager/login")
    public ResponseEntity<AuthDTO.AuthResponse> managerLogin(@RequestBody AuthDTO.LoginRequest request) {
        log.info("Received manager login request for email: {}", request.getEmail());
        AuthDTO.AuthResponse response = authService.managerLogin(request);
        return ResponseEntity.ok(response);
    }

    // ==================== EMPLOYEE ENDPOINTS ====================

    /**
     * Employee signup endpoint
     * POST /api/auth/employee/signup
     */
    @PostMapping("/employee/signup")
    public ResponseEntity<AuthDTO.AuthResponse> employeeSignup(@RequestBody AuthDTO.SignupRequest request) {
        log.info("Received employee signup request for email: {}", request.getEmail());
        AuthDTO.AuthResponse response = authService.employeeSignup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Employee login endpoint
     * POST /api/auth/employee/login
     */
    @PostMapping("/employee/login")
    public ResponseEntity<AuthDTO.AuthResponse> employeeLogin(@RequestBody AuthDTO.LoginRequest request) {
        log.info("Received employee login request for email: {}", request.getEmail());
        AuthDTO.AuthResponse response = authService.employeeLogin(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Employee credential verification endpoint (links the employee's own
     * account to a company using the manager-issued shared username/password)
     * POST /api/auth/employee/verify-credentials
     * Body: EmployeeCredentialsRequest
     */
    @PostMapping("/employee/verify-credentials")
    public ResponseEntity<AuthDTO.AuthResponse> verifyEmployeeCredentials(
            @RequestBody AuthDTO.EmployeeCredentialsRequest request) {
        log.info("Verifying employee credentials for: {}", request.getEmployeeEmail());
        AuthDTO.AuthResponse response = authService.verifyEmployeeCredentials(request);
        return ResponseEntity.ok(response);
    }

    // ==================== GOOGLE OAUTH ENDPOINT ====================

    /**
     * Google OAuth login endpoint
     * POST /api/auth/google/login
     */
    @PostMapping("/google/login")
    public ResponseEntity<AuthDTO.AuthResponse> googleLogin(@RequestBody AuthDTO.GoogleLoginRequest request) {
        log.info("Received Google login request");
        AuthDTO.AuthResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(response);
    }

    // ==================== ADMIN ENDPOINT ====================

    /**
     * Admin login endpoint
     * POST /api/auth/admin/login
     */
    @PostMapping("/admin/login")
    public ResponseEntity<AuthDTO.AuthResponse> adminLogin(@RequestBody AuthDTO.LoginRequest request) {
        log.info("Received admin login request for email: {}", request.getEmail());
        AuthDTO.AuthResponse response = authService.adminLogin(request);
        return ResponseEntity.ok(response);
    }

    // ==================== TOKEN REFRESH ENDPOINT ====================

    /**
     * Refresh token endpoint
     * POST /api/auth/refresh-token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthDTO.AuthResponse> refreshToken(@RequestBody AuthDTO.RefreshTokenRequest request) {
        log.info("Refresh token request");
        AuthDTO.AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logged-in manager marks "I have paid" (WhatsApp / email). Admin still has to approve.
     * POST /api/auth/request-payment
     */
    @PostMapping("/request-payment")
    public ResponseEntity<AuthDTO.AuthResponse> requestPayment(
            java.security.Principal principal,
            @RequestBody(required = false) AuthDTO.PaymentRequest request) {
        if (principal == null) {
            throw new IllegalArgumentException("Login first, then mark payment sent");
        }
        return ResponseEntity.ok(authService.requestPayment(principal.getName(), request));
    }

    /** POST /api/auth/switch-company  body: { "companyId": 2 } */
    @PostMapping("/switch-company")
    public ResponseEntity<AuthDTO.AuthResponse> switchCompany(
            java.security.Principal principal,
            @RequestBody AuthDTO.SwitchCompanyRequest request) {
        return ResponseEntity.ok(authService.switchCompany(principal.getName(), request.getCompanyId()));
    }

    /** GET /api/auth/me */
    @GetMapping("/me")
    public ResponseEntity<AuthDTO.AuthResponse> me(java.security.Principal principal) {
        return ResponseEntity.ok(authService.me(principal.getName()));
    }

    // ==================== HEALTH ENDPOINT ====================

    /**
     * Health check endpoint
     * GET /api/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("CONTROLLER AUTH API is running ✅");
    }
}
