package com.sunwoo.easyledger.ui.statistics;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.common.AppLog;
import com.sunwoo.easyledger.common.SettingKeys;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.entity.CategoryEntity;
import com.sunwoo.easyledger.data.entity.CategorySum;
import com.sunwoo.easyledger.data.entity.DayTotal;
import com.sunwoo.easyledger.data.entity.IncomeExpense;
import com.sunwoo.easyledger.domain.calculator.CategorySpend;
import com.sunwoo.easyledger.domain.calculator.ComparisonWindow;
import com.sunwoo.easyledger.domain.calculator.DayAmount;
import com.sunwoo.easyledger.domain.calculator.StatisticsReport;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsViewModel extends AndroidViewModel {
    public final MutableLiveData<YearMonth> month = new MutableLiveData<>(YearMonth.now());
    public final MutableLiveData<Boolean> includeFixed = new MutableLiveData<>();
    public final MediatorLiveData<StatisticsReport> report = new MediatorLiveData<>();
    private final EasyLedgerApp app;
    private final List<LiveData<?>> sources = new ArrayList<>();
    private List<DayTotal> days;
    private List<CategorySum> sums;
    private List<CategoryEntity> categories;
    private IncomeExpense currentWindow;
    private IncomeExpense previousWindow;
    private IncomeExpense throughToday;
    private boolean ready;
    private final Observer<Boolean> includeObserver = value -> {
        if (value != null) {
            reload();
        }
    };

    public StatisticsViewModel(@NonNull Application application) {
        super(application);
        app = (EasyLedgerApp) application;
        EasyLedgerDatabase.executor().execute(() -> {
            String stored = app.settings().get(SettingKeys.INCLUDE_FIXED, "true");
            includeFixed.postValue(!"false".equals(stored));
        });
    }

    public void attach() {
        if (ready) {
            return;
        }
        ready = true;
        includeFixed.observeForever(includeObserver);
    }

    public void shift(int delta) {
        month.setValue(month.getValue().plusMonths(delta));
        reload();
    }

    public void setIncludeFixed(boolean include) {
        includeFixed.setValue(include);
        EasyLedgerDatabase.executor().execute(() ->
                app.settings().put(SettingKeys.INCLUDE_FIXED, Boolean.toString(include)));
    }

    private void reload() {
        Boolean include = includeFixed.getValue();
        YearMonth selected = month.getValue();
        if (include == null || selected == null) {
            return;
        }
        for (LiveData<?> source : sources) {
            report.removeSource(source);
        }
        sources.clear();
        days = null;
        sums = null;
        categories = null;
        currentWindow = null;
        previousWindow = null;
        throughToday = null;
        LocalDate today = LocalDate.now();
        ComparisonWindow window = ComparisonWindow.of(selected, today);
        LocalDate throughEnd = selected.isAfter(YearMonth.from(today))
                ? selected.atDay(1).minusDays(1)
                : selected.equals(YearMonth.from(today)) ? today : selected.atEndOfMonth();
        String throughStart = throughEnd.isBefore(selected.atDay(1)) ? "0001-01-01" : selected.atDay(1).toString();
        String throughStop = throughEnd.isBefore(selected.atDay(1)) ? "0001-01-01" : throughEnd.toString();
        add(app.transactions().observeFilteredDays(selected.atDay(1).toString(), selected.atEndOfMonth().toString(), include),
                value -> days = value);
        add(app.transactions().observeCategoryExpense(selected.atDay(1).toString(), selected.atEndOfMonth().toString(), include),
                value -> sums = value);
        add(app.categories().observeAll(), value -> categories = value);
        add(app.transactions().observeRange(window.currentStart.toString(), window.currentEnd.toString(), include),
                value -> currentWindow = value);
        add(app.transactions().observeRange(window.previousStart.toString(), window.previousEnd.toString(), include),
                value -> previousWindow = value);
        add(app.transactions().observeRange(throughStart, throughStop, include), value -> throughToday = value);
    }

    private <T> void add(LiveData<T> source, androidx.lifecycle.Observer<T> consumer) {
        sources.add(source);
        report.addSource(source, value -> {
            consumer.onChanged(value);
            publish();
        });
    }

    private void publish() {
        if (days == null || sums == null || categories == null || currentWindow == null
                || previousWindow == null || throughToday == null || month.getValue() == null) {
            return;
        }
        try {
            Map<String, CategoryEntity> byId = new HashMap<>();
            for (CategoryEntity category : categories) {
                byId.put(category.id, category);
            }
            List<CategorySpend> spends = new ArrayList<>();
            for (CategorySum sum : sums) {
                CategoryEntity category = byId.get(sum.categoryId);
                if (category == null) {
                    continue;
                }
                CategoryEntity parent = category.parentId == null ? null : byId.get(category.parentId);
                spends.add(new CategorySpend(
                        category.id,
                        category.parentId,
                        category.name,
                        parent == null ? null : parent.name,
                        sum.total
                ));
            }
            List<DayAmount> dayAmounts = new ArrayList<>();
            for (DayTotal day : days) {
                dayAmounts.add(new DayAmount(LocalDate.parse(day.date), day.income, day.expense));
            }
            report.setValue(StatisticsReport.build(
                    month.getValue(),
                    LocalDate.now(),
                    dayAmounts,
                    spends,
                    currentWindow.income,
                    currentWindow.expense,
                    previousWindow.income,
                    previousWindow.expense,
                    throughToday.expense
            ));
        } catch (RuntimeException exception) {
            AppLog.error("Statistics", "publish", exception);
        }
    }

    @Override
    protected void onCleared() {
        includeFixed.removeObserver(includeObserver);
        for (LiveData<?> source : sources) {
            report.removeSource(source);
        }
        super.onCleared();
    }
}
