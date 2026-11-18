package com.sunwoo.easyledger.domain.recurrence;

import com.sunwoo.easyledger.domain.model.RecurrenceType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RecurrenceCalendar {
    private RecurrenceCalendar() {
    }

    public static String valueFor(RecurrenceType type, LocalDate start) {
        switch (type) {
            case DAILY:
                return "1";
            case WEEKLY:
                return start.getDayOfWeek().name();
            case MONTHLY:
                return Integer.toString(start.getDayOfMonth());
            case YEARLY:
                return String.format(Locale.ROOT, "%02d-%02d", start.getMonthValue(), start.getDayOfMonth());
            default:
                throw new IllegalArgumentException("지원하지 않는 반복 주기입니다.");
        }
    }

    public static String describe(RecurrenceType type, LocalDate start) {
        switch (type) {
            case DAILY:
                return "매일";
            case WEEKLY:
                return "매주 " + koreanDay(start.getDayOfWeek());
            case MONTHLY:
                int day = start.getDayOfMonth();
                if (day >= 29) {
                    return "매월 " + day + "일 (짧은 달은 말일)";
                }
                return "매월 " + day + "일";
            case YEARLY:
                if (start.getMonthValue() == 2 && start.getDayOfMonth() == 29) {
                    return "매년 2월 29일 (평년은 2월 28일)";
                }
                return "매년 " + start.getMonthValue() + "월 " + start.getDayOfMonth() + "일";
            default:
                return "";
        }
    }

    public static String koreanDay(DayOfWeek day) {
        switch (day) {
            case SUNDAY:
                return "일요일";
            case MONDAY:
                return "월요일";
            case TUESDAY:
                return "화요일";
            case WEDNESDAY:
                return "수요일";
            case THURSDAY:
                return "목요일";
            case FRIDAY:
                return "금요일";
            case SATURDAY:
                return "토요일";
            default:
                return "";
        }
    }

    public static List<LocalDate> dates(
            RecurrenceType type,
            String recurrenceValue,
            LocalDate anchorStart,
            LocalDate rangeStart,
            LocalDate rangeEndInclusive
    ) {
        if (type == null || anchorStart == null || rangeStart == null || rangeEndInclusive == null) {
            throw new IllegalArgumentException("반복 날짜를 계산할 수 없습니다.");
        }
        if (rangeStart.isAfter(rangeEndInclusive)) {
            return Collections.emptyList();
        }
        switch (type) {
            case DAILY:
                return daily(anchorStart, rangeStart, rangeEndInclusive);
            case WEEKLY:
                return weekly(anchorStart, recurrenceValue, rangeStart, rangeEndInclusive);
            case MONTHLY:
                return monthly(anchorStart, recurrenceValue, rangeStart, rangeEndInclusive);
            case YEARLY:
                return yearly(anchorStart, recurrenceValue, rangeStart, rangeEndInclusive);
            default:
                throw new IllegalArgumentException("지원하지 않는 반복 주기입니다.");
        }
    }

    public static List<LocalDate> missing(FixedRule rule, LocalDate horizonEnd, Set<LocalDate> existing) {
        if (rule == null || !rule.active || horizonEnd == null) {
            return Collections.emptyList();
        }
        LocalDate rangeStart = rule.startDate.isAfter(rule.effectiveFrom) ? rule.startDate : rule.effectiveFrom;
        LocalDate rangeEnd = horizonEnd;
        if (rule.endDate != null && rule.endDate.isBefore(rangeEnd)) {
            rangeEnd = rule.endDate;
        }
        List<LocalDate> all = dates(
                rule.recurrenceType,
                rule.recurrenceValue,
                rule.startDate,
                rangeStart,
                rangeEnd
        );
        List<LocalDate> missing = new ArrayList<>();
        for (LocalDate date : all) {
            if (!existing.contains(date)) {
                missing.add(date);
            }
        }
        return missing;
    }

    private static List<LocalDate> daily(LocalDate anchor, LocalDate start, LocalDate end) {
        LocalDate cursor = anchor.isAfter(start) ? anchor : start;
        List<LocalDate> out = new ArrayList<>();
        while (!cursor.isAfter(end)) {
            out.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return out;
    }

    private static List<LocalDate> weekly(LocalDate anchor, String value, LocalDate start, LocalDate end) {
        DayOfWeek expected = DayOfWeek.valueOf(value);
        if (anchor.getDayOfWeek() != expected) {
            throw new IllegalArgumentException("매주 반복의 시작 요일이 맞지 않습니다.");
        }
        LocalDate cursor = anchor;
        if (cursor.isBefore(start)) {
            long days = ChronoUnit.DAYS.between(cursor, start);
            long steps = days / 7;
            cursor = cursor.plusDays(steps * 7);
            if (cursor.isBefore(start)) {
                cursor = cursor.plusDays(7);
            }
        }
        List<LocalDate> out = new ArrayList<>();
        while (!cursor.isAfter(end)) {
            out.add(cursor);
            cursor = cursor.plusDays(7);
        }
        return out;
    }

    private static List<LocalDate> monthly(LocalDate anchor, String value, LocalDate start, LocalDate end) {
        int anchorDay = Integer.parseInt(value);
        if (anchorDay < 1 || anchorDay > 31) {
            throw new IllegalArgumentException("매월 반복일이 올바르지 않습니다.");
        }
        YearMonth cursor = YearMonth.from(anchor);
        YearMonth last = YearMonth.from(end);
        List<LocalDate> out = new ArrayList<>();
        while (!cursor.isAfter(last)) {
            int day = Math.min(anchorDay, cursor.lengthOfMonth());
            LocalDate date = cursor.atDay(day);
            if (!date.isBefore(anchor) && !date.isBefore(start) && !date.isAfter(end)) {
                out.add(date);
            }
            cursor = cursor.plusMonths(1);
        }
        return out;
    }

    private static List<LocalDate> yearly(LocalDate anchor, String value, LocalDate start, LocalDate end) {
        String[] parts = value.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("매년 반복일이 올바르지 않습니다.");
        }
        int month = Integer.parseInt(parts[0]);
        int day = Integer.parseInt(parts[1]);
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            throw new IllegalArgumentException("매년 반복일이 올바르지 않습니다.");
        }
        List<LocalDate> out = new ArrayList<>();
        for (int year = anchor.getYear(); year <= end.getYear(); year++) {
            int resolved = Math.min(day, YearMonth.of(year, month).lengthOfMonth());
            LocalDate date = LocalDate.of(year, month, resolved);
            if (!date.isBefore(anchor) && !date.isBefore(start) && !date.isAfter(end)) {
                out.add(date);
            }
        }
        return out;
    }
}
