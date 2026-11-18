package com.sunwoo.easyledger.data.backup;

import android.content.Context;

import com.sunwoo.easyledger.common.UserMessages;
import com.sunwoo.easyledger.data.dao.AppSettingDao;
import com.sunwoo.easyledger.data.dao.CategoryDao;
import com.sunwoo.easyledger.data.dao.FixedOccurrenceDao;
import com.sunwoo.easyledger.data.dao.FixedTransactionDao;
import com.sunwoo.easyledger.data.dao.TransactionDao;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.entity.AppSettingEntity;
import com.sunwoo.easyledger.data.entity.CategoryEntity;
import com.sunwoo.easyledger.data.entity.FixedOccurrenceEntity;
import com.sunwoo.easyledger.data.entity.FixedTransactionEntity;
import com.sunwoo.easyledger.data.entity.TransactionEntity;
import com.sunwoo.easyledger.data.repository.CategoryRepository;
import com.sunwoo.easyledger.data.repository.FixedTransactionRepository;
import com.sunwoo.easyledger.domain.backup.BackupArchive;
import com.sunwoo.easyledger.domain.backup.BackupFiles;
import com.sunwoo.easyledger.domain.backup.BackupFormatException;
import com.sunwoo.easyledger.domain.backup.BackupPreview;
import com.sunwoo.easyledger.domain.backup.BackupValidator;
import com.sunwoo.easyledger.domain.backup.CsvCodec;
import com.sunwoo.easyledger.domain.backup.ValidatedBackup;
import com.sunwoo.easyledger.domain.model.UserVisibleException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BackupRepository {
    private final Context context;
    private final EasyLedgerDatabase database;
    private final CategoryRepository categoryRepository;
    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;
    private final FixedTransactionDao fixedDao;
    private final FixedOccurrenceDao occurrenceDao;
    private final AppSettingDao settingDao;

    public BackupRepository(Context context, EasyLedgerDatabase database, CategoryRepository categoryRepository) {
        this.context = context.getApplicationContext();
        this.database = database;
        this.categoryRepository = categoryRepository;
        transactionDao = database.transactionDao();
        categoryDao = database.categoryDao();
        fixedDao = database.fixedTransactionDao();
        occurrenceDao = database.fixedOccurrenceDao();
        settingDao = database.appSettingDao();
    }

    public byte[] exportZip() {
        try {
            return BackupArchive.zip(sheets());
        } catch (BackupFormatException exception) {
            throw new UserVisibleException("백업 파일을 만들지 못했습니다.");
        }
    }

    public BackupPreview preview(byte[] zipBytes) {
        try {
            return BackupValidator.validate(BackupArchive.unzip(limit(zipBytes))).preview;
        } catch (BackupFormatException exception) {
            throw new UserVisibleException(exception.getMessage());
        }
    }

    public void restore(byte[] zipBytes) {
        synchronized (FixedTransactionRepository.LOCK) {
            try {
                ValidatedBackup backup = BackupValidator.validate(BackupArchive.unzip(limit(zipBytes)));
                byte[] safety = BackupArchive.zip(sheets());
                writeBytes(safetyFile(), safety);
                long oldFingerprint = fingerprintFromDatabase();
                long newFingerprint = fingerprintFromBackup(backup);
                writeMarker(oldFingerprint, newFingerprint);
                database.runInTransaction(() -> replace(backup));
                markerFile().delete();
            } catch (UserVisibleException exception) {
                markerFile().delete();
                throw exception;
            } catch (BackupFormatException exception) {
                markerFile().delete();
                throw new UserVisibleException(exception.getMessage());
            } catch (RuntimeException exception) {
                markerFile().delete();
                com.sunwoo.easyledger.common.AppLog.error("Backup", "restore", exception);
                throw new UserVisibleException(UserMessages.GENERIC);
            }
        }
    }

    public void recoverIfNeeded() {
        File marker = markerFile();
        if (!marker.exists()) {
            return;
        }
        Map<String, String> values = readMarker(marker);
        if (!"APPLYING".equals(values.get("state"))) {
            return;
        }
        long current = fingerprintFromDatabase();
        long oldFingerprint = parseLong(values.get("old"));
        long fresh = parseLong(values.get("new"));
        if (current == oldFingerprint || current == fresh) {
            marker.delete();
            return;
        }
        File safety = safetyFile();
        if (!safety.exists()) {
            writeFailed();
            return;
        }
        try {
            byte[] bytes = readBytes(safety);
            ValidatedBackup backup = BackupValidator.validate(BackupArchive.unzip(bytes));
            database.runInTransaction(() -> replace(backup));
            marker.delete();
        } catch (Exception exception) {
            com.sunwoo.easyledger.common.AppLog.error("Backup", "recover", exception);
            writeFailed();
        }
    }

    private void replace(ValidatedBackup backup) {
        occurrenceDao.deleteAll();
        transactionDao.deleteAll();
        fixedDao.deleteAll();
        categoryRepository.deleteAllCategories();
        settingDao.deleteAll();
        insertCategories(backup.categories);
        for (String[] row : backup.settings) {
            AppSettingEntity setting = new AppSettingEntity();
            setting.key = row[0];
            setting.value = row[1];
            setting.updatedAt = Long.parseLong(row[2]);
            settingDao.put(setting);
        }
        for (String[] row : backup.fixedTransactions) {
            fixedDao.insert(fixedFrom(row));
        }
        for (String[] row : backup.transactions) {
            transactionDao.insert(transactionFrom(row));
        }
        for (String[] row : backup.occurrences) {
            occurrenceDao.insert(occurrenceFrom(row));
        }
    }

    private void insertCategories(List<String[]> rows) {
        Set<String> done = new HashSet<>();
        List<String[]> pending = new ArrayList<>(rows);
        int guard = 0;
        while (!pending.isEmpty()) {
            if (guard++ > rows.size() + 2) {
                throw new UserVisibleException("카테고리 상위 연결이 올바르지 않습니다.");
            }
            boolean progressed = false;
            for (int i = pending.size() - 1; i >= 0; i--) {
                String[] row = pending.get(i);
                if (row[3].isEmpty() || done.contains(row[3])) {
                    categoryDao.insert(categoryFrom(row));
                    done.add(row[0]);
                    pending.remove(i);
                    progressed = true;
                }
            }
            if (!progressed) {
                throw new UserVisibleException("카테고리 상위 연결이 올바르지 않습니다.");
            }
        }
    }

    private Map<String, byte[]> sheets() throws BackupFormatException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        List<String[]> categories = new ArrayList<>();
        for (CategoryEntity entity : categoryDao.all()) {
            categories.add(new String[] {
                    entity.id, entity.name, entity.type, empty(entity.parentId), entity.icon,
                    Integer.toString(entity.displayOrder), Boolean.toString(entity.hidden),
                    Boolean.toString(entity.favorite),
                    Long.toString(entity.quickAmount1), Long.toString(entity.quickAmount2),
                    Long.toString(entity.quickAmount3),
                    Long.toString(entity.createdAt), Long.toString(entity.updatedAt)
            });
        }
        List<String[]> transactions = new ArrayList<>();
        for (TransactionEntity entity : transactionDao.all()) {
            transactions.add(new String[] {
                    entity.id, entity.type, Long.toString(entity.amount), entity.transactionDate,
                    entity.transactionTime, entity.categoryId, entity.paymentMethod, empty(entity.memo),
                    empty(entity.fixedTransactionId), empty(entity.occurrenceKey), Boolean.toString(entity.fixed),
                    Long.toString(entity.createdAt), Long.toString(entity.updatedAt)
            });
        }
        List<String[]> fixed = new ArrayList<>();
        for (FixedTransactionEntity entity : fixedDao.all()) {
            fixed.add(new String[] {
                    entity.id, entity.type, entity.name, Long.toString(entity.amount), entity.categoryId,
                    entity.paymentMethod, entity.recurrenceType, entity.recurrenceValue, entity.startDate,
                    empty(entity.endDate), Boolean.toString(entity.active), entity.effectiveFrom,
                    Integer.toString(entity.ruleVersion), Long.toString(entity.createdAt), Long.toString(entity.updatedAt)
            });
        }
        List<String[]> occurrences = new ArrayList<>();
        for (FixedOccurrenceEntity entity : occurrenceDao.all()) {
            occurrences.add(new String[] {
                    entity.id, entity.fixedTransactionId, entity.occurrenceDate, empty(entity.transactionId),
                    entity.status, Integer.toString(entity.ruleVersion), Long.toString(entity.createdAt)
            });
        }
        List<String[]> settings = new ArrayList<>();
        for (AppSettingEntity entity : settingDao.all()) {
            settings.add(new String[] {entity.key, entity.value, Long.toString(entity.updatedAt)});
        }
        String now = Long.toString(System.currentTimeMillis());
        List<String[]> info = new ArrayList<>();
        info.add(new String[] {"format_version", Integer.toString(BackupFiles.FORMAT_VERSION)});
        info.add(new String[] {"schema_version", Integer.toString(BackupFiles.SCHEMA_VERSION)});
        info.add(new String[] {"app_name", "EasyLedger"});
        info.add(new String[] {"exported_at", now});
        files.put(BackupFiles.CATEGORIES, CsvCodec.write(BackupFiles.CATEGORY_COLUMNS, categories));
        files.put(BackupFiles.TRANSACTIONS, CsvCodec.write(BackupFiles.TRANSACTION_COLUMNS, transactions));
        files.put(BackupFiles.FIXED_TRANSACTIONS, CsvCodec.write(BackupFiles.FIXED_COLUMNS, fixed));
        files.put(BackupFiles.FIXED_OCCURRENCES, CsvCodec.write(BackupFiles.OCCURRENCE_COLUMNS, occurrences));
        files.put(BackupFiles.SETTINGS, CsvCodec.write(BackupFiles.SETTING_COLUMNS, settings));
        files.put(BackupFiles.BACKUP_INFO, CsvCodec.write(BackupFiles.INFO_COLUMNS, info));
        return files;
    }

    private static CategoryEntity categoryFrom(String[] row) {
        CategoryEntity entity = new CategoryEntity();
        entity.id = row[0];
        entity.name = row[1].trim();
        entity.type = row[2];
        entity.parentId = row[3].isEmpty() ? null : row[3];
        entity.icon = row[4];
        entity.displayOrder = Integer.parseInt(row[5]);
        entity.hidden = truth(row[6]);
        entity.favorite = truth(row[7]);
        entity.quickAmount1 = Long.parseLong(row[8]);
        entity.quickAmount2 = Long.parseLong(row[9]);
        entity.quickAmount3 = Long.parseLong(row[10]);
        entity.createdAt = Long.parseLong(row[11]);
        entity.updatedAt = Long.parseLong(row[12]);
        return entity;
    }

    private static TransactionEntity transactionFrom(String[] row) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = row[0];
        entity.type = row[1];
        entity.amount = Long.parseLong(row[2]);
        entity.transactionDate = row[3];
        entity.transactionTime = row[4];
        entity.categoryId = row[5];
        entity.paymentMethod = row[6];
        entity.memo = row[7];
        entity.fixedTransactionId = row[8].isEmpty() ? null : row[8];
        entity.occurrenceKey = row[9].isEmpty() ? null : row[9];
        entity.fixed = truth(row[10]);
        entity.createdAt = Long.parseLong(row[11]);
        entity.updatedAt = Long.parseLong(row[12]);
        return entity;
    }

    private static FixedTransactionEntity fixedFrom(String[] row) {
        FixedTransactionEntity entity = new FixedTransactionEntity();
        entity.id = row[0];
        entity.type = row[1];
        entity.name = row[2].trim();
        entity.amount = Long.parseLong(row[3]);
        entity.categoryId = row[4];
        entity.paymentMethod = row[5];
        entity.recurrenceType = row[6];
        entity.recurrenceValue = row[7];
        entity.startDate = row[8];
        entity.endDate = row[9].isEmpty() ? null : row[9];
        entity.active = truth(row[10]);
        entity.effectiveFrom = row[11];
        entity.ruleVersion = Integer.parseInt(row[12]);
        entity.createdAt = Long.parseLong(row[13]);
        entity.updatedAt = Long.parseLong(row[14]);
        return entity;
    }

    private static FixedOccurrenceEntity occurrenceFrom(String[] row) {
        FixedOccurrenceEntity entity = new FixedOccurrenceEntity();
        entity.id = row[0];
        entity.fixedTransactionId = row[1];
        entity.occurrenceDate = row[2];
        entity.transactionId = row[3].isEmpty() ? null : row[3];
        entity.status = row[4];
        entity.ruleVersion = Integer.parseInt(row[5]);
        entity.createdAt = Long.parseLong(row[6]);
        return entity;
    }

    private long fingerprintFromDatabase() {
        List<TransactionEntity> transactions = new ArrayList<>(transactionDao.all());
        transactions.sort((left, right) -> left.id.compareTo(right.id));
        long value = 17;
        for (TransactionEntity transaction : transactions) {
            value = value * 31 + transaction.amount + transaction.id.hashCode();
        }
        return value * 31 + categoryDao.count();
    }

    private static long fingerprintFromBackup(ValidatedBackup backup) {
        List<String[]> rows = new ArrayList<>(backup.transactions);
        rows.sort((left, right) -> left[0].compareTo(right[0]));
        long value = 17;
        for (String[] row : rows) {
            value = value * 31 + Long.parseLong(row[2]) + row[0].hashCode();
        }
        return value * 31 + backup.categories.size();
    }

    private void writeMarker(long oldFingerprint, long newFingerprint) {
        String body = "state=APPLYING\nold=" + oldFingerprint + "\nnew=" + newFingerprint + "\n";
        writeBytes(markerFile(), body.getBytes(StandardCharsets.UTF_8));
    }

    private void writeFailed() {
        writeBytes(markerFile(), "state=FAILED\n".getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, String> readMarker(File file) {
        Map<String, String> values = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int split = line.indexOf('=');
                if (split > 0) {
                    values.put(line.substring(0, split), line.substring(split + 1));
                }
            }
        } catch (Exception exception) {
            return Collections.emptyMap();
        }
        return values;
    }

    private static long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (RuntimeException exception) {
            return Long.MIN_VALUE;
        }
    }

    private static byte[] limit(byte[] zipBytes) throws BackupFormatException {
        if (zipBytes == null || zipBytes.length == 0 || zipBytes.length > BackupFiles.MAX_ZIP_BYTES) {
            throw new BackupFormatException("백업 파일 크기가 올바르지 않습니다.");
        }
        return zipBytes;
    }

    private File markerFile() {
        return new File(context.getFilesDir(), "restore_marker.txt");
    }

    private File safetyFile() {
        return new File(context.getFilesDir(), "restore_safety.zip");
    }

    private static void writeBytes(File file, byte[] bytes) {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
            output.getFD().sync();
        } catch (Exception exception) {
            throw new UserVisibleException(UserMessages.STORAGE);
        }
    }

    private static byte[] readBytes(File file) throws Exception {
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            int offset = 0;
            while (offset < buffer.length) {
                int read = input.read(buffer, offset, buffer.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
            return buffer;
        }
    }

    private static String empty(String value) {
        return value == null ? "" : value;
    }

    private static boolean truth(String raw) {
        return "true".equals(raw) || "1".equals(raw);
    }
}
