package com.sunwoo.easyledger.domain.model;

public enum OccurrenceStatus {
    GENERATED,
    DELETED,
    SKIPPED;

    public static OccurrenceStatus fromStorage(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("발생 상태가 없습니다.");
        }
        return OccurrenceStatus.valueOf(raw.trim());
    }
}
