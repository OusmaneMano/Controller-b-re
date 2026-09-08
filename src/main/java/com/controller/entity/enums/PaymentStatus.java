package com.controller.entity.enums;

public enum PaymentStatus {
    PENDING("Awaiting admin confirmation"),
    CONFIRMED("Payment confirmed"),
    FAILED("Payment failed"),
    CANCELLED("Payment cancelled");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
