package com.controller.entity.enums;

/**
 * Enum representing the status of a user account
 */
public enum UserStatus {
    ACTIVE,                    // Account is active and can be used
    PENDING_APPROVAL,          // Account is waiting for approval (e.g., payment verification)
    SUSPENDED,                 // Account is temporarily suspended
    INACTIVE,                  // Account is inactive
    DELETED                    // Account has been deleted
}
