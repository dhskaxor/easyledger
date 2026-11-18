package com.sunwoo.easyledger.domain.recurrence;

import com.sunwoo.easyledger.domain.model.RecurrenceType;

import java.time.LocalDate;
import java.util.Objects;

public final class FixedRule {
    public final String id;
    public final String type;
    public final String name;
    public final long amount;
    public final String categoryId;
    public final String paymentMethod;
    public final RecurrenceType recurrenceType;
    public final String recurrenceValue;
    public final LocalDate startDate;
    public final LocalDate endDate;
    public final boolean active;
    public final LocalDate effectiveFrom;
    public final int ruleVersion;

    public FixedRule(
            String id,
            String type,
            String name,
            long amount,
            String categoryId,
            String paymentMethod,
            RecurrenceType recurrenceType,
            String recurrenceValue,
            LocalDate startDate,
            LocalDate endDate,
            boolean active,
            LocalDate effectiveFrom,
            int ruleVersion
    ) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.amount = amount;
        this.categoryId = categoryId;
        this.paymentMethod = paymentMethod;
        this.recurrenceType = recurrenceType;
        this.recurrenceValue = recurrenceValue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
        this.effectiveFrom = effectiveFrom;
        this.ruleVersion = ruleVersion;
    }

    public FixedRule withVersion(int version, LocalDate effective) {
        return new FixedRule(
                id,
                type,
                name,
                amount,
                categoryId,
                paymentMethod,
                recurrenceType,
                recurrenceValue,
                startDate,
                endDate,
                active,
                effective,
                version
        );
    }

    public boolean sameGeneratedFields(FixedRule other) {
        return amount == other.amount
                && Objects.equals(type, other.type)
                && Objects.equals(categoryId, other.categoryId)
                && Objects.equals(paymentMethod, other.paymentMethod)
                && recurrenceType == other.recurrenceType
                && Objects.equals(recurrenceValue, other.recurrenceValue)
                && Objects.equals(startDate, other.startDate)
                && Objects.equals(endDate, other.endDate);
    }
}
