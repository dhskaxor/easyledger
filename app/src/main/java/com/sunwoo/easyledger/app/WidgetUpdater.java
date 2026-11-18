package com.sunwoo.easyledger.app;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public final class WidgetUpdater {
    private WidgetUpdater() {
    }

    public static void updateAll(Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        int[] ids = manager.getAppWidgetIds(new ComponentName(app, TodayExpenseWidgetProvider.class));
        if (ids == null || ids.length == 0) {
            return;
        }
        Intent intent = new Intent(app, TodayExpenseWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        app.sendBroadcast(intent);
    }
}
