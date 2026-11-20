package com.sunwoo.easyledger.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.entity.TxListItem;
import com.sunwoo.easyledger.domain.calculator.Money;
import com.sunwoo.easyledger.domain.model.TransactionType;
import com.sunwoo.easyledger.ui.MainActivity;

import java.time.LocalDate;
import java.util.List;

public class TodayExpenseWidgetProvider extends AppWidgetProvider {
    public static final String EXTRA_OPEN_INPUT = "com.sunwoo.easyledger.OPEN_INPUT";
    public static final String EXTRA_OPEN_CALENDAR = "com.sunwoo.easyledger.OPEN_CALENDAR";
    public static final String EXTRA_SELECTED_DATE = "com.sunwoo.easyledger.SELECTED_DATE";

    private enum SizeMode {
        COMPACT,
        MEDIUM,
        WIDE
    }

    /** 위젯 크기에 맞춘 sp 스케일. */
    private static final class TextScale {
        final float title;
        final float amount;
        final float label;
        final float body;
        final float button;
        final float meta;

        TextScale(float title, float amount, float label, float body, float button, float meta) {
            this.title = title;
            this.amount = amount;
            this.label = label;
            this.body = body;
            this.button = button;
            this.meta = meta;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateOne(context, appWidgetManager, id);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(
            Context context,
            AppWidgetManager appWidgetManager,
            int appWidgetId,
            Bundle newOptions
    ) {
        updateOne(context, appWidgetManager, appWidgetId);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int appWidgetId) {
        EasyLedgerDatabase.executor().execute(() -> {
            EasyLedgerApp app = (EasyLedgerApp) context.getApplicationContext();
            String today = LocalDate.now().toString();
            long expense = app.transactions().expenseOnDate(today);
            long income = app.transactions().incomeOnDate(today);
            List<TxListItem> items = app.transactions().listOnDate(today);
            Bundle options = manager.getAppWidgetOptions(appWidgetId);
            SizeMode mode = resolveMode(options);
            TextScale scale = resolveTextScale(options, mode);
            RemoteViews views = buildViews(context, mode, scale, expense, income, items);
            manager.updateAppWidget(appWidgetId, views);
        });
    }

    private static SizeMode resolveMode(Bundle options) {
        if (options == null) {
            return SizeMode.MEDIUM;
        }
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110);
        if (minWidth < 180 || minHeight < 100) {
            return SizeMode.COMPACT;
        }
        if (minWidth >= 300 && minHeight >= 110) {
            return SizeMode.WIDE;
        }
        return SizeMode.MEDIUM;
    }

    /**
     * 위젯 minWidth/minHeight(dp)와 레이아웃 모드로 글자 크기를 계산합니다.
     * 기준 폭 250dp ≈ 1.0, 대략 0.88~1.28 사이로 클램프합니다.
     */
    private static TextScale resolveTextScale(Bundle options, SizeMode mode) {
        int minWidth = options == null
                ? 250
                : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
        int minHeight = options == null
                ? 110
                : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110);

        float widthFactor = minWidth / 250f;
        float heightFactor = minHeight / 120f;
        float factor = (widthFactor * 0.7f) + (heightFactor * 0.3f);
        factor = Math.max(0.88f, Math.min(1.28f, factor));

        // 모드별 기본 크기(sp)에 배율 적용
        float baseAmount;
        float baseTitle;
        float baseButton;
        switch (mode) {
            case COMPACT:
                baseAmount = 14f;
                baseTitle = 11f;
                baseButton = 11f;
                break;
            case WIDE:
                baseAmount = 17f;
                baseTitle = 12f;
                baseButton = 12f;
                break;
            case MEDIUM:
            default:
                baseAmount = 15f;
                baseTitle = 12f;
                baseButton = 12f;
                break;
        }

        return new TextScale(
                baseTitle * factor,
                baseAmount * factor,
                11f * factor,
                12f * factor,
                baseButton * factor,
                11f * factor
        );
    }

    private static int layoutFor(SizeMode mode) {
        switch (mode) {
            case COMPACT:
                return R.layout.widget_today_compact;
            case WIDE:
                return R.layout.widget_today_wide;
            case MEDIUM:
            default:
                return R.layout.widget_today_expense;
        }
    }

    private static int maxItemsFor(SizeMode mode) {
        switch (mode) {
            case COMPACT:
                return 0;
            case WIDE:
                return 6;
            case MEDIUM:
            default:
                return 3;
        }
    }

    private static void setSp(RemoteViews views, int viewId, float sp) {
        views.setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, sp);
    }

    private static RemoteViews buildViews(
            Context context,
            SizeMode mode,
            TextScale scale,
            long expense,
            long income,
            List<TxListItem> items
    ) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutFor(mode));
        views.setTextViewText(R.id.widget_expense, Money.won(expense));
        views.setTextViewText(R.id.widget_income, Money.won(income));
        int count = items == null ? 0 : items.size();
        views.setTextViewText(R.id.widget_count, context.getString(R.string.widget_today_count, count));

        // RemoteViews는 XML text가 누락되는 기기가 있어 버튼 문구/색을 코드에서 강제 설정
        views.setTextViewText(R.id.widget_add, context.getString(R.string.widget_quick_input_short));
        views.setTextColor(R.id.widget_add, Color.WHITE);
        views.setTextViewText(R.id.widget_detail, context.getString(R.string.widget_view_detail_short));
        views.setTextColor(R.id.widget_detail, ContextCompat.getColor(context, R.color.brand));

        setSp(views, R.id.widget_expense, scale.amount);
        setSp(views, R.id.widget_income, scale.amount);
        setSp(views, R.id.widget_count, scale.meta);
        setSp(views, R.id.widget_add, scale.button);
        setSp(views, R.id.widget_detail, scale.button);
        setSp(views, R.id.widget_title, scale.title);
        if (mode != SizeMode.COMPACT) {
            setSp(views, R.id.widget_list_title, scale.meta);
            setSp(views, R.id.widget_empty, scale.body);
            setSp(views, R.id.widget_more, scale.meta);
        }

        int maxItems = maxItemsFor(mode);
        views.removeAllViews(R.id.widget_list);
        if (mode == SizeMode.COMPACT || maxItems <= 0) {
            views.setViewVisibility(R.id.widget_list, View.GONE);
            views.setViewVisibility(R.id.widget_empty, View.GONE);
            views.setViewVisibility(R.id.widget_more, View.GONE);
        } else if (count == 0) {
            views.setViewVisibility(R.id.widget_list, View.GONE);
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE);
            views.setViewVisibility(R.id.widget_more, View.GONE);
        } else {
            views.setViewVisibility(R.id.widget_list, View.VISIBLE);
            views.setViewVisibility(R.id.widget_empty, View.GONE);
            int shown = Math.min(count, maxItems);
            int incomeColor = ContextCompat.getColor(context, R.color.income_deep);
            int expenseColor = ContextCompat.getColor(context, R.color.expense_deep);
            for (int i = 0; i < shown; i++) {
                TxListItem item = items.get(i);
                RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_today_item);
                String name = item.categoryName == null || item.categoryName.isEmpty()
                        ? context.getString(R.string.category)
                        : item.categoryName;
                boolean isIncome = TransactionType.INCOME.name().equals(item.type);
                row.setTextViewText(R.id.item_name, name);
                row.setTextViewText(R.id.item_amount,
                        (isIncome ? "+" : "-") + Money.format(item.amount));
                row.setTextColor(R.id.item_amount, isIncome ? incomeColor : expenseColor);
                setSp(row, R.id.item_name, scale.body);
                setSp(row, R.id.item_amount, scale.body);
                views.addView(R.id.widget_list, row);
            }
            if (count > maxItems) {
                views.setViewVisibility(R.id.widget_more, View.VISIBLE);
                views.setTextViewText(R.id.widget_more,
                        context.getString(R.string.widget_today_more, count - maxItems));
            } else {
                views.setViewVisibility(R.id.widget_more, View.GONE);
            }
        }

        views.setOnClickPendingIntent(R.id.widget_root, openCalendar(context, 11));
        views.setOnClickPendingIntent(R.id.widget_detail, openCalendar(context, 12));
        views.setOnClickPendingIntent(R.id.widget_add, openInput(context, 13));
        views.setOnClickPendingIntent(R.id.widget_more, openCalendar(context, 14));
        return views;
    }

    private static PendingIntent openInput(Context context, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_OPEN_INPUT, true);
        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent openCalendar(Context context, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_OPEN_CALENDAR, true);
        intent.putExtra(EXTRA_SELECTED_DATE, LocalDate.now().toString());
        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
