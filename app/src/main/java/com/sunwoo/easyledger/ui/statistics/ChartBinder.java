package com.sunwoo.easyledger.ui.statistics;

import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.domain.calculator.NamedAmount;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;

public final class ChartBinder {
    private static final int[] SLICE_COLORS = {
            R.color.chart_1, R.color.chart_2, R.color.chart_3, R.color.chart_4,
            R.color.chart_5, R.color.chart_6, R.color.chart_7, R.color.chart_8
    };

    private ChartBinder() {
    }

    public static void bindBars(BarChart chart, long[] income, long[] expense) {
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.getLegend().setTextColor(chartText(chart));
        chart.getXAxis().setTextColor(chartText(chart));
        chart.getAxisLeft().setTextColor(chartText(chart));
        chart.getXAxis().setGranularity(1f);
        long max = 0;
        for (int i = 0; i < income.length; i++) {
            max = Math.max(max, Math.max(income[i], expense[i]));
        }
        float scale = max > 16_000_000L ? 10000f : 1f;
        List<BarEntry> incomeEntries = new ArrayList<>();
        List<BarEntry> expenseEntries = new ArrayList<>();
        for (int i = 0; i < income.length; i++) {
            incomeEntries.add(new BarEntry(i + 1, income[i] / scale));
            expenseEntries.add(new BarEntry(i + 1, expense[i] / scale));
        }
        BarDataSet incomeSet = new BarDataSet(incomeEntries, scale == 1f ? "수입" : "수입(만 원)");
        BarDataSet expenseSet = new BarDataSet(expenseEntries, scale == 1f ? "지출" : "지출(만 원)");
        incomeSet.setColor(ContextCompat.getColor(chart.getContext(), R.color.income));
        expenseSet.setColor(ContextCompat.getColor(chart.getContext(), R.color.expense));
        incomeSet.setDrawValues(false);
        expenseSet.setDrawValues(false);
        BarData data = new BarData(incomeSet, expenseSet);
        data.setBarWidth(0.35f);
        chart.setData(data);
        chart.groupBars(0.5f, 0.2f, 0.05f);
        chart.invalidate();
    }

    public static void bindDonut(PieChart chart, List<NamedAmount> slices) {
        chart.getDescription().setEnabled(false);
        chart.setDrawEntryLabels(false);
        chart.setHoleRadius(58f);
        chart.setTransparentCircleRadius(62f);
        chart.setCenterText("지출");
        chart.setCenterTextColor(chartText(chart));
        chart.getLegend().setTextColor(chartText(chart));
        chart.getLegend().setOrientation(Legend.LegendOrientation.VERTICAL);
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int index = 0;
        for (NamedAmount slice : slices) {
            if (slice.amount <= 0) {
                continue;
            }
            entries.add(new PieEntry((float) slice.amount, slice.name));
            colors.add(ContextCompat.getColor(chart.getContext(), SLICE_COLORS[index % SLICE_COLORS.length]));
            index++;
        }
        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(colors);
        set.setDrawValues(false);
        chart.setData(new PieData(set));
        chart.invalidate();
    }

    private static int chartText(android.view.View view) {
        int color = view.getContext().getColor(R.color.neutral);
        return color == 0 ? Color.DKGRAY : color;
    }
}
