package com.sunwoo.easyledger.ui.calendar;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.data.entity.DayTotal;
import com.sunwoo.easyledger.data.entity.TxListItem;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class CalendarViewModel extends AndroidViewModel {
    public final MutableLiveData<YearMonth> month = new MutableLiveData<>();
    public final MutableLiveData<LocalDate> selected = new MutableLiveData<>();
    public final MediatorLiveData<List<DayTotal>> totals = new MediatorLiveData<>();
    public final MediatorLiveData<List<TxListItem>> dayItems = new MediatorLiveData<>();
    private final EasyLedgerApp app;
    private LiveData<List<DayTotal>> totalSource;
    private LiveData<List<TxListItem>> daySource;

    public CalendarViewModel(@NonNull Application application) {
        super(application);
        app = (EasyLedgerApp) application;
        LocalDate today = LocalDate.now();
        month.setValue(YearMonth.from(today));
        selected.setValue(today);
        reloadMonth();
        reloadDay();
    }

    public void shift(int delta) {
        YearMonth next = month.getValue().plusMonths(delta);
        int day = Math.min(selected.getValue().getDayOfMonth(), next.lengthOfMonth());
        month.setValue(next);
        selected.setValue(next.atDay(day));
        reloadMonth();
        reloadDay();
    }

    public void goToday() {
        LocalDate today = LocalDate.now();
        month.setValue(YearMonth.from(today));
        selected.setValue(today);
        reloadMonth();
        reloadDay();
    }

    public void select(LocalDate date) {
        if (!YearMonth.from(date).equals(month.getValue())) {
            month.setValue(YearMonth.from(date));
            reloadMonth();
        }
        selected.setValue(date);
        reloadDay();
    }

    private void reloadMonth() {
        YearMonth current = month.getValue();
        if (totalSource != null) {
            totals.removeSource(totalSource);
        }
        totalSource = app.transactions().observeDayTotals(
                current.atDay(1).toString(), current.atEndOfMonth().toString());
        totals.addSource(totalSource, totals::setValue);
    }

    private void reloadDay() {
        if (daySource != null) {
            dayItems.removeSource(daySource);
        }
        daySource = app.transactions().observeDay(selected.getValue().toString());
        dayItems.addSource(daySource, dayItems::setValue);
    }
}
