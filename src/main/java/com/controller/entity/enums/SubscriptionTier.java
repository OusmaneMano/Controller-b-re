package com.controller.entity.enums;

public enum SubscriptionTier {
    SOLO(5.0, "0-1 employees"),
    SMALL(10.0, "1-3 employees"),
    GROWING(15.0, "4-10 employees"),
    MEDIUM(20.0, "11-20 employees"),
    LARGE(30.0, "21-50 employees"),
    ENTERPRISE(40.0, "51-100 employees");

    private final Double monthlyPrice;
    private final String description;

    SubscriptionTier(Double monthlyPrice, String description) {
        this.monthlyPrice = monthlyPrice;
        this.description = description;
    }

    public Double getMonthlyPrice() {
        return monthlyPrice;
    }

    public String getDescription() {
        return description;
    }
}
