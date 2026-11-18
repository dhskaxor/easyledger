package com.sunwoo.easyledger.domain.calculator;

import com.sunwoo.easyledger.domain.model.Limits;

public final class Money {
    private Money() {
    }

    public static String format(long amount) {
        boolean negative = amount < 0;
        String digits = Long.toString(negative ? Math.abs(amount) : amount);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                builder.append(',');
            }
            builder.append(digits.charAt(i));
        }
        return (negative ? "-" : "") + builder;
    }

    public static String won(long amount) {
        return format(amount) + "원";
    }

    public static String signedWon(long amount) {
        if (amount > 0) {
            return "+" + format(amount) + "원";
        }
        if (amount < 0) {
            return "-" + format(Math.abs(amount)) + "원";
        }
        return "0원";
    }

    public static String appendDigits(String current, String addition) {
        String base = current == null || current.equals("0") ? "" : current;
        String next = base + addition;
        if (next.length() > Limits.AMOUNT_DIGITS) {
            return normalize(current);
        }
        return normalize(next);
    }

    public static String backspace(String current) {
        if (current == null || current.isEmpty() || current.equals("0")) {
            return "0";
        }
        String next = current.substring(0, current.length() - 1);
        return normalize(next);
    }

    public static String normalize(String digits) {
        if (digits == null || digits.isEmpty()) {
            return "0";
        }
        int index = 0;
        while (index < digits.length() - 1 && digits.charAt(index) == '0') {
            index++;
        }
        String trimmed = digits.substring(index);
        if (trimmed.length() > Limits.AMOUNT_DIGITS) {
            return trimmed.substring(0, Limits.AMOUNT_DIGITS);
        }
        return trimmed;
    }

    public static long toAmount(String digits) {
        String normalized = normalize(digits);
        if (!normalized.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("금액은 숫자만 입력할 수 있습니다.");
        }
        return Long.parseLong(normalized);
    }

    public static long parsePlain(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("금액이 없습니다.");
        }
        String text = raw.trim();
        if (text.isEmpty() || text.indexOf('.') >= 0 || text.indexOf(',') >= 0) {
            throw new IllegalArgumentException("금액이 올바르지 않습니다.");
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("금액이 올바르지 않습니다.");
        }
    }
}
