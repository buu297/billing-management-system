package com.buu297.billing.model;

public enum PaymentMethod {
    CREDIT_CARD("Credit / Debit Card"),
    BANK_TRANSFER("Bank Transfer"),
    VNPAY_QR("VNPay / QR Pay"),
    CASH("Cash (Counter)"),
    FINANCIAL_AID("Financial Aid / Grant");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PaymentMethod fromString(String text) {
        if (text == null) return CASH;
        for (PaymentMethod m : values()) {
            if (m.name().equalsIgnoreCase(text) || m.displayName.equalsIgnoreCase(text)) {
                return m;
            }
        }
        return CASH;
    }
}
