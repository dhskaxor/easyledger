package com.sunwoo.easyledger.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
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

/**
 * 크기가 달라도 기능은 같습니다. (+ 입력 / 달력 / 오늘 요약)
 * 공간에 따라 화면 구성과 목록 개수만 달라지며, 줄어들어도 깨지지 않게 맞춥니다.
 */
public class TodayExpenseWidgetProvider extends AppWidgetProvider {
    public static final String EXTRA_OPEN_INPUT = "com.sunwoo.easyledger.OPEN_INPUT";
    public static final String EXTRA_OPEN_CALENDAR = "com.sunwoo.easyledger.OPEN_CALENDAR";
    public static final String EXTRA_SELECTED_DATE = "com.sunwoo.easyledger.SELECTED_DATE";

    private enum SizeMode {
        MICRO,
        COMPACT,
        STANDARD,
        TALL,
        WIDE,
        WIDE_TALL
    }

    private static final class TextScale {
        final float title;
        final float amount;
        final float body;
        final float meta;

        TextScale(float title, float amount, float body, float meta) {
            this.title = title;
            this.amount = amount;
            this.body = body;
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
            RemoteViews views = buildViews(context, mode, scale, options, expense, income, items);
            manager.updateAppWidget(appWidgetId, views);
        });
    }

    private static SizeMode resolveMode(Bundle options) {
        int minWidth = option(options, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
        int minHeight = option(options, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110);

        if (minHeight < 80 || (minWidth < 150 && minHeight < 110)) {
            return SizeMode.MICRO;
        }
        if (minWidth >= 300) {
            return minHeight >= 170 ? SizeMode.WIDE_TALL : SizeMode.WIDE;
        }
        if (minWidth < 180 || minHeight < 105) {
            return SizeMode.COMPACT;
        }
        return minHeight >= 190 ? SizeMode.TALL : SizeMode.STANDARD;
    }

    private static TextScale resolveTextScale(Bundle options, SizeMode mode) {
        int minWidth = option(options, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
        int minHeight = option(options, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110);
        float factor = (minWidth / 250f) * 0.65f + (minHeight / 130f) * 0.35f;
        factor = Math.max(0.86f, Math.min(1.30f, factor));

        float baseAmount;
        float baseTitle;
        switch (mode) {
            case MICRO:
                baseAmount = 15f;
                baseTitle = 10f;
                break;
            case COMPACT:
                baseAmount = 14f;
                baseTitle = 11f;
                break;
            case WIDE:
            case WIDE_TALL:
            case TALL:
                baseAmount = 16f;
                baseTitle = 12f;
                break;
            case STANDARD:
            default:
                baseAmount = 15f;
                baseTitle = 12f;
                break;
        }
        return new TextScale(baseTitle * factor, baseAmount * factor, 12f * factor, 11f * factor);
    }

    private static int layoutFor(SizeMode mode) {
        switch (mode) {
            case MICRO:
                return R.layout.widget_today_micro;
            case COMPACT:
                return R.layout.widget_today_compact;
            case TALL:
                return R.layout.widget_today_tall;
            case WIDE:
            case WIDE_TALL:
                return R.layout.widget_today_wide;
            case STANDARD:
            default:
                return R.layout.widget_today_expense;
        }
    }

    private static int maxItemsFor(SizeMode mode, Bundle options) {
        int minHeight = option(options, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110);
        switch (mode) {
            case MICRO:
            case COMPACT:
                return 0;
            case STANDARD:
                return minHeight >= 140 ? 3 : 2;
            case TALL:
                if (minHeight >= 280) {
                    return 8;
                }
                if (minHeight >= 230) {
                    return 6;
                }
                return 5;
            case WIDE:
                return minHeight >= 130 ? 5 : 3;
            case WIDE_TALL:
                if (minHeight >= 260) {
                    return 10;
                }
                if (minHeight >= 200) {
                    return 8;
                }
                return 6;
            default:
                return 3;
        }
    }

    private static boolean showsList(SizeMode mode) {
        return mode != SizeMode.MICRO && mode != SizeMode.COMPACT;
    }

    private static int option(Bundle options, String key, int fallback) {
        if (options == null) {
            return fallback;
        }
        return options.getInt(key, fallback);
    }

    private static void setSp(RemoteViews views, int viewId, float sp) {
        views.setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, sp);
    }

    private static RemoteViews buildViews(
            Context context,
            SizeMode mode,
            TextScale scale,
            Bundle options,
            long expense,
            long income,
            List<TxListItem> items
    ) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutFor(mode));
        int count = items == null ? 0 : items.size();

        views.setTextViewText(R.id.widget_expense, Money.won(expense));
        views.setTextViewText(R.id.widget_count, context.getString(R.string.widget_today_count, count));
        views.setImageViewResource(R.id.widget_add, R.drawable.ic_widget_add);
        views.setImageViewResource(R.id.widget_detail, R.drawable.ic_widget_calendar);

        if (mode == SizeMode.MICRO) {
            if (income > 0) {
                views.setViewVisibility(R.id.widget_income, View.VISIBLE);
                views.setTextViewText(R.id.widget_income,
                        context.getString(R.string.income) + " " + Money.won(income));
            } else {
                views.setViewVisibility(R.id.widget_income, View.GONE);
                views.setTextViewText(R.id.widget_income, Money.won(0));
            }
        } else {
            views.setViewVisibility(R.id.widget_income, View.VISIBLE);
            views.setTextViewText(R.id.widget_income, Money.won(income));
        }

        setSp(views, R.id.widget_expense, scale.amount);
        setSp(views, R.id.widget_income, mode == SizeMode.MICRO ? scale.meta : scale.amount);
        setSp(views, R.id.widget_count, scale.meta);
        setSp(views, R.id.widget_title, scale.title);

        int maxItems = maxItemsFor(mode, options);
        views.removeAllViews(R.id.widget_list);

        if (!showsList(mode) || maxItems <= 0) {
            views.setViewVisibility(R.id.widget_list, View.GONE);
            views.setViewVisibility(R.id.widget_empty, View.GONE);
            views.setViewVisibility(R.id.widget_more, View.GONE);
            if (mode == SizeMode.MICRO || mode == SizeMode.COMPACT) {
                // stubs may exist; keep gone
            } else {
                views.setViewVisibility(R.id.widget_list_title, View.GONE);
            }
        } else {
            setSp(views, R.id.widget_list_title, scale.meta);
            setSp(views, R.id.widget_empty, scale.body);
            setSp(views, R.id.widget_more, scale.meta);
            views.setViewVisibility(R.id.widget_list_title, View.VISIBLE);
            if (count == 0) {
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
