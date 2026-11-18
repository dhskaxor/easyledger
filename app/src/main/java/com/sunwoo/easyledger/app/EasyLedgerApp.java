package com.sunwoo.easyledger.app;

import android.app.Application;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.sunwoo.easyledger.common.AppLog;
import com.sunwoo.easyledger.data.backup.BackupRepository;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.repository.CategoryRepository;
import com.sunwoo.easyledger.data.repository.FixedTransactionRepository;
import com.sunwoo.easyledger.data.repository.SettingsRepository;
import com.sunwoo.easyledger.data.repository.TransactionRepository;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

public class EasyLedgerApp extends Application {
    private CategoryRepository categories;
    private TransactionRepository transactions;
    private FixedTransactionRepository fixedTransactions;
    private SettingsRepository settings;
    private BackupRepository backup;

    @Override
    public void onCreate() {
        super.onCreate();
        EasyLedgerDatabase database = EasyLedgerDatabase.get(this);
        categories = new CategoryRepository(database);
        transactions = new TransactionRepository(database);
        fixedTransactions = new FixedTransactionRepository(database);
        settings = new SettingsRepository(database);
        backup = new BackupRepository(this, database, categories);
        WorkManager workManager = WorkManager.getInstance(this);
        PeriodicWorkRequest fixedRequest = new PeriodicWorkRequest.Builder(
                FixedTransactionWorker.class, 1, TimeUnit.DAYS).build();
        workManager.enqueueUniquePeriodicWork(
                "easyledger-fixed",
                ExistingPeriodicWorkPolicy.KEEP,
                fixedRequest
        );
        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                ReminderWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayHours(), TimeUnit.HOURS)
                .build();
        workManager.enqueueUniquePeriodicWork(
                "easyledger-reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest
        );
        EasyLedgerDatabase.executor().execute(this::startup);
    }

    private void startup() {
        try {
            categories.ensureSeeded();
            backup.recoverIfNeeded();
            fixedTransactions.generateMissing(LocalDate.now());
            WidgetUpdater.updateAll(this);
        } catch (RuntimeException exception) {
            AppLog.error("EasyLedger", "startup", exception);
        }
    }

    /** Prefer evening reminder (~20:00). */
    private static long initialDelayHours() {
        int hour = java.time.LocalTime.now().getHour();
        int target = 20;
        int delay = target - hour;
        if (delay <= 0) {
            delay += 24;
        }
        return delay;
    }

    public CategoryRepository categories() {
        return categories;
    }

    public TransactionRepository transactions() {
        return transactions;
    }

    public FixedTransactionRepository fixedTransactions() {
        return fixedTransactions;
    }

    public SettingsRepository settings() {
        return settings;
    }

    public BackupRepository backup() {
        return backup;
    }
}
