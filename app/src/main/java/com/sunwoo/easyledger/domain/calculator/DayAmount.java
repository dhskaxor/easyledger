package com.sunwoo.easyledger.domain.calculator;

import java.time.LocalDate;

public final class DayAmount {
    public final LocalDate date;
    public final long income;
    public final long expense;

    public DayAmount(LocalDate date, long income, long expense) {
        this.date = date;
        this.income = income;
        this.expense = expense;
    }
}
