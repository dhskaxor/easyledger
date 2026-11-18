package com.sunwoo.easyledger.domain.model;

public enum PaymentMethod {
    CASH,
    CARD;

    public static PaymentMethod fromStorage(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("결제수단이 없습니다.");
        }
        return PaymentMethod.valueOf(raw.trim());
    }

    public String label() {
        return this == CARD ? "카드" : "현금";
    }
}
