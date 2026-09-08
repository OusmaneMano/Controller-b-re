package com.controller.entity.enums;

public enum PaymentType {
    SETUP_FEE("One-time setup fee"),
    MONTHLY_MAINTENANCE("Monthly subscription fee");

    private final String description;

    PaymentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
