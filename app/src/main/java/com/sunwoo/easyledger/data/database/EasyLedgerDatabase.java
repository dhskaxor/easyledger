package com.sunwoo.easyledger.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.sunwoo.easyledger.data.dao.AppSettingDao;
import com.sunwoo.easyledger.data.dao.CategoryDao;
import com.sunwoo.easyledger.data.dao.CategoryLibraryDao;
import com.sunwoo.easyledger.data.dao.FixedOccurrenceDao;
import com.sunwoo.easyledger.data.dao.FixedTransactionDao;
import com.sunwoo.easyledger.data.dao.SchemaMetadataDao;
import com.sunwoo.easyledger.data.dao.TransactionDao;
import com.sunwoo.easyledger.data.entity.AppSettingEntity;
import com.sunwoo.easyledger.data.entity.CategoryEntity;
import com.sunwoo.easyledger.data.entity.CategoryLibraryEntity;
import com.sunwoo.easyledger.data.entity.FixedOccurrenceEntity;
import com.sunwoo.easyledger.data.entity.FixedTransactionEntity;
import com.sunwoo.easyledger.data.entity.SchemaMetadataEntity;
import com.sunwoo.easyledger.data.entity.TransactionEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                TransactionEntity.class,
                CategoryEntity.class,
                CategoryLibraryEntity.class,
                FixedTransactionEntity.class,
                FixedOccurrenceEntity.class,
                AppSettingEntity.class,
                SchemaMetadataEntity.class
        },
        version = 2,
        exportSchema = true
)
public abstract class EasyLedgerDatabase extends RoomDatabase {
    private static volatile EasyLedgerDatabase instance;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "easyledger-db");
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    });

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE categories ADD COLUMN quick_amount_1 INTEGER NOT NULL DEFAULT 0");
            database.execSQL(
                    "ALTER TABLE categories ADD COLUMN quick_amount_2 INTEGER NOT NULL DEFAULT 0");
            database.execSQL(
                    "ALTER TABLE categories ADD COLUMN quick_amount_3 INTEGER NOT NULL DEFAULT 0");
        }
    };

    public abstract TransactionDao transactionDao();

    public abstract CategoryDao categoryDao();

    public abstract CategoryLibraryDao categoryLibraryDao();

    public abstract FixedTransactionDao fixedTransactionDao();

    public abstract FixedOccurrenceDao fixedOccurrenceDao();

    public abstract AppSettingDao appSettingDao();

    public abstract SchemaMetadataDao schemaMetadataDao();

    public static ExecutorService executor() {
        return EXECUTOR;
    }

    public static EasyLedgerDatabase get(@NonNull Context context) {
        if (instance == null) {
            synchronized (EasyLedgerDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            EasyLedgerDatabase.class,
                            "easyledger.db"
                    ).addMigrations(MIGRATION_1_2).build();
                }
            }
        }
        return instance;
    }
}
