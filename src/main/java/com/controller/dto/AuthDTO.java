package com.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthDTO {

    // ==================== SIGNUP REQUESTS ====================

    /**
     * Generic signup request used by both manager and employee signup endpoints
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignupRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String country;
        private String password;
        private String confirmPassword;
        private String companyName;
        private Boolean acceptedTerms;
    }

    /**
     * Manager-specific signup request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagerSignupRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String country;
        private String password;
        private String confirmPassword;
        private String companyName;
        private boolean termsAccepted;
    }

    /**
     * Employee-specific signup request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeSignupRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String password;
        private String confirmPassword;
        private boolean termsAccepted;
    }

    // ==================== LOGIN REQUESTS ====================

    /**
     * Generic login request for manager and employee
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        private String email;
        private String password;
    }

    /**
     * Admin-specific login request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminLoginRequest {
        private String email;
        private String password;
    }

    /**
     * Google OAuth login request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoogleLoginRequest {
        private String idToken;
    }

    /**
     * Employee credentials verification request (manager's credentials to verify employee)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeCredentialsRequest {
        private String employeeEmail;
        private String managerUsername;
        private String managerPassword;
    }

    /**
     * Refresh token request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshTokenRequest {
        private String refreshToken;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRequest {
        private String note;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SwitchCompanyRequest {
        private Long companyId;
    }

    // ==================== RESPONSES ====================

    /**
     * Generic auth response containing token and user info
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthResponse {
        private String token;
        private String refreshToken;
        private UserInfo user;
        private CompanyInfo company;
        private java.util.List<MembershipInfo> memberships;
        private String message;
    }

    /**
     * Signup response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SignupResponse {
        private Long userId;
        private String message;
        private Boolean success;
    }

    /**
     * Login response containing token and user details
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginResponse {
        private String token;
        private Long userId;
        private String email;
        private String role;
        private String status;
    }

    /**
     * User information DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String role;
        private String status;
        private String paymentNote;
    }

    /**
     * Company information DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompanyInfo {
        private Long id;
        private String name;
        private String status;
        private Boolean demo;
        private String roleInCompany;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MembershipInfo {
        private Long companyId;
        private String companyName;
        private String industry;
        private String role;
        private Boolean demo;
        private String status;
    }
}

