package com.buu297.billing.model;

public enum ItemType {
    TUITION_COURSE("Tuition Course Fee"),
    LAB_FEE("Laboratory & Computing Access"),
    LIBRARY_FEE("Library & Digital Resources"),
    HEALTH_INSURANCE("Student Health Insurance"),
    CAMPUS_FACILITY("Campus Facilities & Amenities"),
    SCHOLARSHIP_DISCOUNT("Scholarship / Tuition Waiver"),
    LATE_PENALTY("Late Payment Penalty");

    private final String displayName;

    ItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
