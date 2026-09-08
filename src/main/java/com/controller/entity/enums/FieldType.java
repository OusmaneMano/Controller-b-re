package com.controller.entity.enums;

public enum FieldType {
    TEXT("Text input"),
    NUMBER("Numeric input"),
    DATE("Date picker"),
    DROPDOWN("Dropdown selection"),
    PHONE("Phone number"),
    EMAIL("Email address"),
    TEXTAREA("Long text area"),
    FILE("File upload");

    private final String description;

    FieldType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
