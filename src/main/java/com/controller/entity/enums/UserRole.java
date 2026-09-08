package com.controller.entity.enums;

public enum UserRole {
    MANAGER("Manager - Creates companies and manages data"),
    EMPLOYEE("Employee - Enters and manages own data within 24h"),
    ADMIN("Admin - Manages all companies and payments");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
