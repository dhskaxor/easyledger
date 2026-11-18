package com.sunwoo.easyledger.domain.calculator;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StatisticsReport {
    public final long totalIncome;
    public final long totalExpense;
    public final long balance;
    public final String incomeChangeText;
    public final String expenseChangeText;
    public final String comparisonRangeText;
    public final long dailyAverageExpense;
    public final String dailyAverageText;
    public final String peakDayText;
    public final String peakCategoryText;
    public final List<NamedAmount> detailRows;
    public final List<NamedAmount> chartSlices;
    public final long[] incomeByDay;
    public final long[] expenseByDay;

    public StatisticsReport(
            long totalIncome,
            long totalExpense,
            long balance,
            String incomeChangeText,
            String expenseChangeText,
            String comparisonRangeText,
            long dailyAverageExpense,
            String dailyAverageText,
            String peakDayText,
            String peakCategoryText,
            List<NamedAmount> detailRows,
            List<NamedAmount> chartSlices,
            long[] incomeByDay,
            long[] expenseByDay
    ) {
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.balance = balance;
        this.incomeChangeText = incomeChangeText;
        this.expenseChangeText = expenseChangeText;
        this.comparisonRangeText = comparisonRangeText;
        this.dailyAverageExpense = dailyAverageExpense;
        this.dailyAverageText = dailyAverageText;
        this.peakDayText = peakDayText;
        this.peakCategoryText = peakCategoryText;
        this.detailRows = detailRows;
        this.chartSlices = chartSlices;
        this.incomeByDay = incomeByDay;
        this.expenseByDay = expenseByDay;
    }

    public static StatisticsReport build(
            YearMonth month,
            java.time.LocalDate today,
            List<DayAmount> monthDays,
            List<CategorySpend> spends,
            long windowIncome,
            long windowExpense,
            long previousIncome,
            long previousExpense,
            long expenseThroughToday
    ) {
        int length = month.lengthOfMonth();
        long[] incomeByDay = new long[length];
        long[] expenseByDay = new long[length];
        long totalIncome = 0;
        long totalExpense = 0;
        DayAmount peak = null;
        for (DayAmount day : monthDays) {
            if (day.date == null || !YearMonth.from(day.date).equals(month)) {
                continue;
            }
            int index = day.date.getDayOfMonth() - 1;
            incomeByDay[index] = day.income;
            expenseByDay[index] = day.expense;
            totalIncome += day.income;
            totalExpense += day.expense;
            if (day.expense > 0 && (peak == null || day.expense > peak.expense
                    || (day.expense == peak.expense && day.date.isBefore(peak.date)))) {
                peak = day;
            }
        }
        List<NamedAmount> rolled = rollup(spends);
        long categoryTotal = 0;
        for (NamedAmount row : rolled) {
            categoryTotal += row.amount;
        }
        List<NamedAmount> detail = new ArrayList<>();
        for (NamedAmount row : rolled) {
            detail.add(new NamedAmount(row.name, row.amount, ChangeText.percent(row.amount, categoryTotal)));
        }
        List<NamedAmount> slices = groupSmall(detail, categoryTotal);
        String peakCategory = detail.isEmpty() || detail.get(0).amount <= 0
                ? "지출이 없습니다"
                : detail.get(0).name;
        int divisor = ComparisonWindow.averageDivisor(month, today);
        long average = divisor == 0 ? 0 : expenseThroughToday / divisor;
        String averageText = divisor == 0 ? "해당 없음" : Money.won(average);
        String peakDay = peak == null
                ? "지출이 없습니다"
                : peak.date.getMonthValue() + "월 " + peak.date.getDayOfMonth() + "일";
        ComparisonWindow window = ComparisonWindow.of(month, today);
        return new StatisticsReport(
                totalIncome,
                totalExpense,
                totalIncome - totalExpense,
                ChangeText.format(windowIncome, previousIncome),
                ChangeText.format(windowExpense, previousExpense),
                window.previousLabel(),
                average,
                averageText,
                peakDay,
                peakCategory,
                detail,
                slices,
                incomeByDay,
                expenseByDay
        );
    }

    static List<NamedAmount> rollup(List<CategorySpend> spends) {
        Map<String, Long> sums = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        for (CategorySpend spend : spends) {
            if (spend.amount <= 0) {
                continue;
            }
            boolean hasParent = spend.parentId != null && !spend.parentId.isEmpty();
            String id = hasParent ? spend.parentId : spend.categoryId;
            String name = hasParent && spend.parentName != null && !spend.parentName.isEmpty()
                    ? spend.parentName
                    : spend.name;
            sums.put(id, sums.getOrDefault(id, 0L) + spend.amount);
            names.put(id, name);
        }
        List<NamedAmount> rows = new ArrayList<>();
        for (Map.Entry<String, Long> entry : sums.entrySet()) {
            rows.add(new NamedAmount(names.get(entry.getKey()), entry.getValue(), ""));
        }
        rows.sort(Comparator.comparingLong((NamedAmount row) -> row.amount).reversed()
                .thenComparing(row -> row.name));
        return rows;
    }

    static List<NamedAmount> groupSmall(List<NamedAmount> detail, long total) {
        List<NamedAmount> large = new ArrayList<>();
        long smallSum = 0;
        int smallCount = 0;
        for (NamedAmount row : detail) {
            if (ChangeText.underThreePercent(row.amount, total)) {
                smallSum += row.amount;
                smallCount++;
            } else {
                large.add(new NamedAmount(row.name, row.amount, row.percentText));
            }
        }
        if (smallCount >= 2) {
            large.add(new NamedAmount("그 외", smallSum, ChangeText.percent(smallSum, total)));
            return large;
        }
        return new ArrayList<>(detail);
    }
}
