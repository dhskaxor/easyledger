package com.sunwoo.easyledger.domain.model;

public enum RecurrenceType {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    public static RecurrenceType fromStorage(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("반복 주기가 없습니다.");
        }
        return RecurrenceType.valueOf(raw.trim());
    }

    public String label() {
        switch (this) {
            case DAILY:
                return "매일";
            case WEEKLY:
                return "매주";
            case MONTHLY:
                return "매월";
            case YEARLY:
                return "매년";
            default:
                return name();
        }
    }
}
