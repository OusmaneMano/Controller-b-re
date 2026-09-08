package com.controller.entity.enums;

public enum CompanyStatus {
    PENDING("Awaiting payment verification"),
    ACTIVE("Company is active"),
    SUSPENDED("Company suspended"),
    CANCELLED("Company cancelled");

    private final String description;

    CompanyStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
