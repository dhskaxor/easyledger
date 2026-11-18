package com.sunwoo.easyledger.domain.calculator;

import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class StatisticsReportTest {
    @Test
    public void ongoingMonthComparesSameDayAndSkipsZeroRatio() {
        ComparisonWindow window = ComparisonWindow.of(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 16));
        assertEquals(LocalDate.of(2026, 11, 1), window.currentStart);
        assertEquals(LocalDate.of(2026, 11, 16), window.currentEnd);
        assertEquals(LocalDate.of(2026, 10, 1), window.previousStart);
        assertEquals(LocalDate.of(2026, 10, 16), window.previousEnd);

        ComparisonWindow closed = ComparisonWindow.of(YearMonth.of(2026, 10), LocalDate.of(2026, 11, 16));
        assertEquals(LocalDate.of(2026, 10, 31), closed.currentEnd);
        assertEquals(LocalDate.of(2026, 9, 30), closed.previousEnd);

        ComparisonWindow march = ComparisonWindow.of(YearMonth.of(2026, 3), LocalDate.of(2026, 3, 31));
        assertEquals(LocalDate.of(2026, 2, 28), march.previousEnd);
        assertEquals("비교할 전월 금액이 없습니다", ChangeText.format(5000, 0));
        assertEquals("변화 없음", ChangeText.format(0, 0));
        assertEquals("+1,000원 (+10.0%)", ChangeText.format(11000, 10000));
    }

    @Test
    public void rollsParentsGroupsTinySlicesAndAveragesElapsedDays() {
        StatisticsReport report = StatisticsReport.build(
                YearMonth.of(2026, 11),
                LocalDate.of(2026, 11, 16),
                Arrays.asList(
                        new DayAmount(LocalDate.of(2026, 11, 2), 0, 7000),
                        new DayAmount(LocalDate.of(2026, 11, 16), 30000, 2000)
                ),
                Arrays.asList(
                        new CategorySpend("lunch", "food", "점심", "식비", 6000),
                        new CategorySpend("food", null, "식비", null, 3000),
                        new CategorySpend("snack", null, "간식", null, 100),
                        new CategorySpend("gum", null, "껌", null, 50)
                ),
                30000,
                9000,
                20000,
                9000,
                9000
        );
        assertEquals(30000L, report.totalIncome);
        assertEquals(9000L, report.totalExpense);
        assertEquals(21000L, report.balance);
        assertEquals(9000L / 16L, report.dailyAverageExpense);
        assertEquals("11월 2일", report.peakDayText);
        assertEquals("식비", report.peakCategoryText);
        assertEquals(3, report.detailRows.size());
        assertEquals(9000L, report.detailRows.get(0).amount);
        assertEquals("변화 없음", report.expenseChangeText);
        assertEquals(1, report.chartSlices.stream().filter(slice -> "그 외".equals(slice.name)).count());
    }

    @Test
    public void emptyExpenseHasNoPeak() {
        StatisticsReport report = StatisticsReport.build(
                YearMonth.of(2025, 1),
                LocalDate.of(2026, 11, 16),
                Collections.emptyList(),
                Collections.emptyList(),
                0, 0, 0, 0, 0
        );
        assertEquals(0L, report.balance);
        assertEquals("지출이 없습니다", report.peakDayText);
        assertEquals("지출이 없습니다", report.peakCategoryText);
        assertEquals(31, report.expenseByDay.length);
    }
}
