package com.buu297.billing.model;

public enum BillingStatus {
    UNPAID("Unpaid"),
    PARTIAL("Partially Paid"),
    PAID("Paid"),
    OVERDUE("Overdue");

    private final String displayName;

    BillingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static BillingStatus fromString(String text) {
        if (text == null) return UNPAID;
        for (BillingStatus s : values()) {
            if (s.name().equalsIgnoreCase(text) || s.displayName.equalsIgnoreCase(text)) {
                return s;
            }
        }
        return UNPAID;
    }
}
