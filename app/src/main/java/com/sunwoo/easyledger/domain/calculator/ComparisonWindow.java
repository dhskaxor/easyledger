package com.sunwoo.easyledger.domain.calculator;

import java.time.LocalDate;
import java.time.YearMonth;

public final class ComparisonWindow {
    public final LocalDate currentStart;
    public final LocalDate currentEnd;
    public final LocalDate previousStart;
    public final LocalDate previousEnd;

    public ComparisonWindow(
            LocalDate currentStart,
            LocalDate currentEnd,
            LocalDate previousStart,
            LocalDate previousEnd
    ) {
        this.currentStart = currentStart;
        this.currentEnd = currentEnd;
        this.previousStart = previousStart;
        this.previousEnd = previousEnd;
    }

    public static ComparisonWindow of(YearMonth month, LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previous = month.minusMonths(1);
        LocalDate currentStart = month.atDay(1);
        LocalDate currentEnd;
        LocalDate previousEnd;
        if (month.equals(currentMonth)) {
            currentEnd = today;
            int day = Math.min(today.getDayOfMonth(), previous.lengthOfMonth());
            previousEnd = previous.atDay(day);
        } else {
            currentEnd = month.atEndOfMonth();
            previousEnd = previous.atEndOfMonth();
        }
        return new ComparisonWindow(currentStart, currentEnd, previous.atDay(1), previousEnd);
    }

    public static int averageDivisor(YearMonth month, LocalDate today) {
        YearMonth current = YearMonth.from(today);
        if (month.isAfter(current)) {
            return 0;
        }
        if (month.equals(current)) {
            return today.getDayOfMonth();
        }
        return month.lengthOfMonth();
    }

    public String previousLabel() {
        return previousStart.getMonthValue() + "월 " + previousStart.getDayOfMonth() + "일~"
                + previousEnd.getMonthValue() + "월 " + previousEnd.getDayOfMonth() + "일";
    }
}
