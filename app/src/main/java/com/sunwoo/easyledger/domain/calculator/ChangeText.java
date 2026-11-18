package com.sunwoo.easyledger.domain.calculator;

public final class ChangeText {
    private ChangeText() {
    }

    public static String format(long current, long previous) {
        if (previous == 0) {
            if (current == 0) {
                return "변화 없음";
            }
            return "비교할 전월 금액이 없습니다";
        }
        long delta = current - previous;
        if (delta == 0) {
            return "변화 없음";
        }
        String amount = (delta > 0 ? "+" : "-") + Money.format(Math.abs(delta)) + "원";
        try {
            long tenths = Math.multiplyExact(delta, 1000L) / previous;
            return amount + " (" + signedTenths(tenths) + ")";
        } catch (ArithmeticException exception) {
            return amount;
        }
    }

    public static String percent(long amount, long total) {
        if (total <= 0) {
            return "0.0%";
        }
        try {
            long tenths = Math.multiplyExact(amount, 1000L) / total;
            if (tenths < 0) {
                tenths = 0;
            }
            return (tenths / 10) + "." + (tenths % 10) + "%";
        } catch (ArithmeticException exception) {
            return "-";
        }
    }

    public static boolean underThreePercent(long amount, long total) {
        if (total <= 0 || amount <= 0) {
            return false;
        }
        try {
            return Math.multiplyExact(amount, 100L) < Math.multiplyExact(total, 3L);
        } catch (ArithmeticException exception) {
            return false;
        }
    }

    private static String signedTenths(long tenths) {
        String sign = tenths > 0 ? "+" : tenths < 0 ? "-" : "";
        long absolute = Math.abs(tenths);
        return sign + (absolute / 10) + "." + (absolute % 10) + "%";
    }
}
