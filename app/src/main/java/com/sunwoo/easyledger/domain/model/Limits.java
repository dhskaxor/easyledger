package com.sunwoo.easyledger.domain.model;

import java.time.LocalTime;

public final class Limits {
    public static final int MEMO = 200;
    public static final int CATEGORY_NAME = 20;
    public static final int FIXED_NAME = 40;
    public static final int AMOUNT_DIGITS = 12;
    public static final LocalTime FIXED_TRANSACTION_TIME = LocalTime.of(9, 0, 0);
    public static final String DEFAULT_PAYMENT = PaymentMethod.CASH.name();

    private Limits() {
    }
}
