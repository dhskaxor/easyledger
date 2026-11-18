package com.sunwoo.easyledger.domain.model;

public enum TransactionType {
    INCOME,
    EXPENSE;

    public static TransactionType fromStorage(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("거래 유형이 없습니다.");
        }
        return TransactionType.valueOf(raw.trim());
    }

    public String label() {
        return this == INCOME ? "수입" : "지출";
    }
}
