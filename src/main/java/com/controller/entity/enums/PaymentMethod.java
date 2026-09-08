package com.controller.entity.enums;

public enum PaymentMethod {
    BANK_TRANSFER("Bank Transfer"),
    WHATSAPP("WhatsApp Transfer"),
    EMAIL("Email Payment");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
