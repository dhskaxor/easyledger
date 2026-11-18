package com.sunwoo.easyledger.domain.backup;

public final class BackupFiles {
    public static final String TRANSACTIONS = "transactions.csv";
    public static final String CATEGORIES = "categories.csv";
    public static final String FIXED_TRANSACTIONS = "fixed_transactions.csv";
    public static final String FIXED_OCCURRENCES = "fixed_occurrences.csv";
    public static final String SETTINGS = "settings.csv";
    public static final String BACKUP_INFO = "backup_info.csv";
    public static final String[] REQUIRED = {
            TRANSACTIONS,
            CATEGORIES,
            FIXED_TRANSACTIONS,
            FIXED_OCCURRENCES,
            SETTINGS,
            BACKUP_INFO
    };
    public static final int FORMAT_VERSION = 1;
    public static final int SCHEMA_VERSION = 2;
    public static final long MAX_ZIP_BYTES = 32L * 1024L * 1024L;
    public static final long MAX_ENTRY_BYTES = 8L * 1024L * 1024L;
    public static final long MAX_TOTAL_UNCOMPRESSED = 32L * 1024L * 1024L;

    public static final String[] TRANSACTION_COLUMNS = {
            "id", "type", "amount", "transaction_date", "transaction_time", "category_id",
            "payment_method", "memo", "fixed_transaction_id", "occurrence_key", "is_fixed",
            "created_at", "updated_at"
    };
    public static final String[] CATEGORY_COLUMNS = {
            "id", "name", "type", "parent_id", "icon", "display_order", "is_hidden",
            "is_favorite", "quick_amount_1", "quick_amount_2", "quick_amount_3",
            "created_at", "updated_at"
    };
    /** schema 1 백업용. 복원 시 quick_amount 를 0으로 채운다. */
    public static final String[] CATEGORY_COLUMNS_V1 = {
            "id", "name", "type", "parent_id", "icon", "display_order", "is_hidden",
            "is_favorite", "created_at", "updated_at"
    };
    public static final String[] FIXED_COLUMNS = {
            "id", "type", "name", "amount", "category_id", "payment_method", "recurrence_type",
            "recurrence_value", "start_date", "end_date", "is_active", "effective_from",
            "rule_version", "created_at", "updated_at"
    };
    public static final String[] OCCURRENCE_COLUMNS = {
            "id", "fixed_transaction_id", "occurrence_date", "transaction_id", "status",
            "rule_version", "created_at"
    };
    public static final String[] SETTING_COLUMNS = {"key", "value", "updated_at"};
    public static final String[] INFO_COLUMNS = {"key", "value"};

    private BackupFiles() {
    }

    public static void assertSafeEntryName(String name) throws BackupFormatException {
        if (name == null || name.isEmpty()) {
            throw new BackupFormatException("압축 파일 항목이 올바르지 않습니다.");
        }
        if (name.indexOf('\\') >= 0
                || name.indexOf('/') >= 0
                || name.contains("..")
                || name.startsWith(".")) {
            throw new BackupFormatException("압축 파일 경로가 올바르지 않습니다.");
        }
        boolean known = false;
        for (String required : REQUIRED) {
            if (required.equals(name)) {
                known = true;
                break;
            }
        }
        if (!known) {
            throw new BackupFormatException("백업에 알 수 없는 파일이 있습니다.");
        }
    }
}
