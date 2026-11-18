package com.sunwoo.easyledger.ui.statistics;

import com.sunwoo.easyledger.R;

final class ChartBinderColors {
    private static final int[] COLORS = {
            R.color.chart_1, R.color.chart_2, R.color.chart_3, R.color.chart_4,
            R.color.chart_5, R.color.chart_6, R.color.chart_7, R.color.chart_8
    };

    private ChartBinderColors() {
    }

    static int colorAt(int index) {
        return COLORS[Math.floorMod(index, COLORS.length)];
    }
}
