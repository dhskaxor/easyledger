package com.sunwoo.easyledger.domain.model;

import java.time.LocalDate;

public final class Searchable {
    public final String type;
    public final long amount;
    public final LocalDate date;
    public final String categoryId;
    public final String payment;
    public final String memo;

    public Searchable(String type, long amount, LocalDate date, String categoryId, String payment, String memo) {
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.categoryId = categoryId;
        this.payment = payment;
        this.memo = memo;
    }
}
