package com.sports.modules.registration.entity;

import java.util.Locale;

/** API status string to database numeric status mapping. */
public enum RegistrationStatus {
    PENDING(1, "待审核"), APPROVED(2, "已通过"), REJECTED(3, "已拒绝"), CANCELLED(4, "已取消");
    private final int code;
    private final String label;
    RegistrationStatus(int code, String label) { this.code = code; this.label = label; }
    public int getCode() { return code; }
    public String getLabel() { return label; }
    public static RegistrationStatus fromApi(String value) {
        return value == null || value.isBlank() ? null : valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
    public static RegistrationStatus fromCode(Integer code) {
        if (code == null) return null;
        for (RegistrationStatus status : values()) if (status.code == code) return status;
        return null;
    }
}
