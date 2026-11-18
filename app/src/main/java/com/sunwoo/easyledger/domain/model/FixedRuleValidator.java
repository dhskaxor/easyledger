package com.sunwoo.easyledger.domain.model;

import com.sunwoo.easyledger.domain.calculator.Money;

import java.time.LocalDate;

public final class FixedRuleValidator {
    private FixedRuleValidator() {
    }

    public static String validate(
            String type,
            String name,
            long amount,
            String categoryId,
            String categoryType,
            String payment,
            String recurrenceType,
            String recurrenceValue,
            String startDate,
            String endDate
    ) {
        try {
            TransactionType.fromStorage(type);
        } catch (RuntimeException exception) {
            return "거래 유형을 선택해 주세요.";
        }
        if (name == null || name.trim().isEmpty()) {
            return "반복 거래 이름을 입력해 주세요.";
        }
        if (name.trim().length() > Limits.FIXED_NAME) {
            return "이름은 40자까지 입력할 수 있습니다.";
        }
        if (amount <= 0) {
            return "금액은 0보다 커야 합니다.";
        }
        if (categoryId == null || categoryId.isEmpty()) {
            return "카테고리를 선택해 주세요.";
        }
        if (categoryType == null || !categoryType.equals(type)) {
            return "카테고리와 거래 유형이 맞지 않습니다.";
        }
        try {
            PaymentMethod.fromStorage(payment);
        } catch (RuntimeException exception) {
            return "결제수단을 선택해 주세요.";
        }
        RecurrenceType recurrence;
        try {
            recurrence = RecurrenceType.fromStorage(recurrenceType);
        } catch (RuntimeException exception) {
            return "반복 주기를 선택해 주세요.";
        }
        if (!TransactionRules.isDate(startDate)) {
            return "시작일이 올바르지 않습니다.";
        }
        if (endDate != null && !endDate.isEmpty()) {
            if (!TransactionRules.isDate(endDate)) {
                return "종료일이 올바르지 않습니다.";
            }
            if (LocalDate.parse(endDate).isBefore(LocalDate.parse(startDate))) {
                return "종료일은 시작일보다 빠를 수 없습니다.";
            }
        }
        if (recurrenceValue == null || recurrenceValue.isEmpty()) {
            return "반복 기준이 올바르지 않습니다.";
        }
        try {
            com.sunwoo.easyledger.domain.recurrence.RecurrenceCalendar.dates(
                    recurrence,
                    recurrenceValue,
                    LocalDate.parse(startDate),
                    LocalDate.parse(startDate),
                    LocalDate.parse(startDate)
            );
        } catch (RuntimeException exception) {
            return "반복 기준이 시작일과 맞지 않습니다.";
        }
        if (amount > 0 && Money.format(amount).isEmpty()) {
            return "금액이 올바르지 않습니다.";
        }
        return null;
    }
}
