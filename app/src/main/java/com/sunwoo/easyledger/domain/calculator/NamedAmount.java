package com.sunwoo.easyledger.domain.calculator;

public final class NamedAmount {
    public final String name;
    public final long amount;
    public final String percentText;

    public NamedAmount(String name, long amount, String percentText) {
        this.name = name;
        this.amount = amount;
        this.percentText = percentText;
    }
}
