package com.sunwoo.easyledger.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.common.SettingKeys;
import com.sunwoo.easyledger.data.entity.FixedTransactionEntity;
import com.sunwoo.easyledger.domain.model.RecurrenceType;
import com.sunwoo.easyledger.domain.recurrence.RecurrenceCalendar;
import com.sunwoo.easyledger.ui.MainActivity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReminderWorker extends Worker {
    public static final String CHANNEL_ID = "easyledger_reminders";
    private static final int NOTIFY_NO_ENTRY = 1001;
    private static final int NOTIFY_FIXED = 1002;

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        EasyLedgerApp app = (EasyLedgerApp) context;
        ensureChannel(context);
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        boolean noEntryOn = !"0".equals(app.settings().get(SettingKeys.REMINDER_NO_ENTRY, "1"));
        boolean fixedOn = !"0".equals(app.settings().get(SettingKeys.REMINDER_FIXED, "1"));

        if (noEntryOn && app.transactions().countOnDate(today.toString()) == 0) {
            notify(
                    context,
                    NOTIFY_NO_ENTRY,
                    context.getString(R.string.notify_no_entry_title),
                    context.getString(R.string.notify_no_entry_body),
                    true
            );
        }

        if (fixedOn) {
            List<String> dueNames = upcomingFixedNames(app, tomorrow);
            if (!dueNames.isEmpty()) {
                String body = dueNames.size() == 1
                        ? context.getString(R.string.notify_fixed_body_one, dueNames.get(0))
                        : context.getString(R.string.notify_fixed_body_many, dueNames.size());
                notify(
                        context,
                        NOTIFY_FIXED,
                        context.getString(R.string.notify_fixed_title),
                        body,
                        false
                );
            }
        }
        WidgetUpdater.updateAll(context);
        return Result.success();
    }

    private static List<String> upcomingFixedNames(EasyLedgerApp app, LocalDate target) {
        List<String> names = new ArrayList<>();
        for (FixedTransactionEntity entity : app.fixedTransactions().activeRules()) {
            if (entity == null || entity.name == null) {
                continue;
            }
            try {
                RecurrenceType type = RecurrenceType.fromStorage(entity.recurrenceType);
                LocalDate start = LocalDate.parse(entity.startDate);
                LocalDate end = entity.endDate == null || entity.endDate.isEmpty()
                        ? target
                        : LocalDate.parse(entity.endDate);
                if (end.isBefore(target)) {
                    continue;
                }
                List<LocalDate> dates = RecurrenceCalendar.dates(type, entity.recurrenceValue, start, target, target);
                if (!dates.isEmpty()) {
                    names.add(entity.name);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return names;
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notify_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription(context.getString(R.string.notify_channel_desc));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private static void notify(Context context, int id, String title, String body, boolean openInput) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (openInput) {
            intent.putExtra(TodayExpenseWidgetProvider.EXTRA_OPEN_INPUT, true);
        }
        PendingIntent pending = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_ledger)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(context).notify(id, builder.build());
    }
}
