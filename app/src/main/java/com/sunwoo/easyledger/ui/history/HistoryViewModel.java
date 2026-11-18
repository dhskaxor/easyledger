package com.sunwoo.easyledger.ui.history;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.common.DateTimes;
import com.sunwoo.easyledger.data.entity.IncomeExpense;
import com.sunwoo.easyledger.data.entity.TxListItem;
import com.sunwoo.easyledger.domain.model.SearchCriteria;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HistoryViewModel extends AndroidViewModel {
    public final MediatorLiveData<List<TxListItem>> items = new MediatorLiveData<>();
    public final MediatorLiveData<IncomeExpense> monthTotals = new MediatorLiveData<>();
    public final MutableLiveData<String> title = new MutableLiveData<>();
    private final EasyLedgerApp app;
    private YearMonth month = YearMonth.now();
    private LocalDate start = month.atDay(1);
    private LocalDate end = month.atEndOfMonth();
    private String type;
    private String payment;
    private String memo = "";
    private Long minAmount;
    private Long maxAmount;
    private final Set<String> categoryIds = new HashSet<>();
    private LiveData<List<TxListItem>> source;
    private LiveData<IncomeExpense> monthSource;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        app = (EasyLedgerApp) application;
        reload();
    }

    public void shiftMonth(int delta) {
        month = month.plusMonths(delta);
        start = month.atDay(1);
        end = month.atEndOfMonth();
        reload();
    }

    public void setType(String value) {
        type = value;
        reload();
    }

    public String type() {
        return type;
    }

    public YearMonth month() {
        return month;
    }

    public void apply(
            Set<String> categories,
            String memoKeyword,
            Long min,
            Long max,
            LocalDate rangeStart,
            LocalDate rangeEnd,
            String paymentMethod
    ) {
        categoryIds.clear();
        if (categories != null) {
            categoryIds.addAll(categories);
        }
        memo = memoKeyword == null ? "" : memoKeyword.trim();
        minAmount = min;
        maxAmount = max;
        start = rangeStart;
        end = rangeEnd;
        payment = paymentMethod;
        if (start.getDayOfMonth() == 1 && end.equals(YearMonth.from(start).atEndOfMonth())) {
            month = YearMonth.from(start);
        }
        reload();
    }

    public void clearFilters() {
        categoryIds.clear();
        memo = "";
        minAmount = null;
        maxAmount = null;
        payment = null;
        type = null;
        start = month.atDay(1);
        end = month.atEndOfMonth();
        reload();
    }

    public LocalDate start() { return start; }
    public LocalDate end() { return end; }
    public String memo() { return memo; }
    public Long minAmount() { return minAmount; }
    public Long maxAmount() { return maxAmount; }
    public String payment() { return payment; }
    public Set<String> categoryIds() { return new HashSet<>(categoryIds); }

    private void reload() {
        if (source != null) {
            items.removeSource(source);
        }
        if (monthSource != null) {
            monthTotals.removeSource(monthSource);
        }
        int useMemo = memo.isEmpty() ? 0 : 1;
        source = app.transactions().observeSearch(
                start.toString(),
                end.toString(),
                type == null ? 0 : 1,
                type,
                payment == null ? 0 : 1,
                payment,
                minAmount == null ? 0 : 1,
                minAmount == null ? 0 : minAmount,
                maxAmount == null ? 0 : 1,
                maxAmount == null ? 0 : maxAmount,
                useMemo,
                useMemo == 0 ? "%" : SearchCriteria.likePattern(memo),
                categoryIds.isEmpty() ? 1 : 0,
                categoryIds.isEmpty() ? Collections.singletonList("-") : new ArrayList<>(categoryIds)
        );
        items.addSource(source, items::setValue);
        monthSource = app.transactions().observeRange(
                month.atDay(1).toString(),
                month.atEndOfMonth().toString(),
                true
        );
        monthTotals.addSource(monthSource, monthTotals::setValue);
        title.setValue(DateTimes.formatRange(start, end));
    }
}
