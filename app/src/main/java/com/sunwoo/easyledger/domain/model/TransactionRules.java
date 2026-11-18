package com.sunwoo.easyledger.domain.model;

import com.sunwoo.easyledger.domain.calculator.Money;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public final class TransactionRules {
    private TransactionRules() {
    }

    public static String validate(
            String type,
            long amount,
            String date,
            String time,
            String categoryId,
            String categoryType,
            String payment,
            String memo
    ) {
        try {
            TransactionType.fromStorage(type);
        } catch (RuntimeException exception) {
            return "거래 유형을 선택해 주세요.";
        }
        if (amount <= 0) {
            return "금액은 0보다 커야 합니다.";
        }
        if (date == null || !isDate(date)) {
            return "날짜가 올바르지 않습니다.";
        }
        if (time == null || !isTime(time)) {
            return "시간이 올바르지 않습니다.";
        }
        if (categoryId == null || categoryId.trim().isEmpty()) {
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
        if (memo != null && memo.length() > Limits.MEMO) {
            return "메모는 200자까지 입력할 수 있습니다.";
        }
        return null;
    }

    public static String validateDigits(String digits) {
        try {
            long amount = Money.toAmount(digits);
            if (amount <= 0) {
                return "금액은 0보다 커야 합니다.";
            }
            return null;
        } catch (RuntimeException exception) {
            return "금액이 올바르지 않습니다.";
        }
    }

    public static boolean isDate(String value) {
        try {
            LocalDate parsed = LocalDate.parse(value);
            return parsed.toString().equals(value);
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    public static boolean isTime(String value) {
        try {
            LocalTime parsed = LocalTime.parse(value);
            String formatted = String.format(
                    java.util.Locale.ROOT,
                    "%02d:%02d:%02d",
                    parsed.getHour(),
                    parsed.getMinute(),
                    parsed.getSecond()
            );
            return formatted.equals(value);
        } catch (DateTimeParseException exception) {
            return false;
        }
    }
}
