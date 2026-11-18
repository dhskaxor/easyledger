package com.sunwoo.easyledger.app;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sunwoo.easyledger.common.AppLog;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

public class FixedTransactionWorker extends Worker {
    public FixedTransactionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            EasyLedgerDatabase.executor().submit(() -> {
                ((EasyLedgerApp) getApplicationContext()).fixedTransactions().generateMissing(LocalDate.now());
                return null;
            }).get(3, TimeUnit.MINUTES);
            return Result.success();
        } catch (Exception exception) {
            AppLog.error("FixedWorker", "run", exception);
            return Result.retry();
        }
    }
}
