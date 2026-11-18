package com.sunwoo.easyledger.common;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateTimes {
    public static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREA);

    private DateTimes() {
    }

    public static LocalDate today() {
        return LocalDate.now();
    }

    public static LocalTime now() {
        return LocalTime.now().withNano(0);
    }

    public static String formatTime(LocalTime time) {
        return String.format(Locale.ROOT, "%02d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond());
    }

    public static String formatHourMinute(String stored) {
        if (stored == null || stored.length() < 5) {
            return "";
        }
        return stored.substring(0, 5);
    }

    public static String formatDay(LocalDate date) {
        return date.getYear() + "년 " + date.getMonthValue() + "월 " + date.getDayOfMonth() + "일";
    }

    public static String formatMonth(YearMonth month) {
        return month.format(MONTH);
    }

    public static String formatRange(LocalDate start, LocalDate end) {
        if (start.equals(end)) {
            return formatDay(start);
        }
        YearMonth startMonth = YearMonth.from(start);
        if (start.getDayOfMonth() == 1 && end.equals(startMonth.atEndOfMonth())) {
            return formatMonth(startMonth);
        }
        if (start.getYear() == end.getYear()) {
            return start.getMonthValue() + "월 " + start.getDayOfMonth() + "일 ~ "
                    + end.getMonthValue() + "월 " + end.getDayOfMonth() + "일";
        }
        return formatDay(start) + " ~ " + formatDay(end);
    }
}
