package com.sunwoo.easyledger.domain.backup;

public final class BackupPreview {
    public final int transactionCount;
    public final int categoryCount;
    public final int fixedCount;
    public final int occurrenceCount;
    public final int settingCount;
    public final String exportedAt;

    public BackupPreview(
            int transactionCount,
            int categoryCount,
            int fixedCount,
            int occurrenceCount,
            int settingCount,
            String exportedAt
    ) {
        this.transactionCount = transactionCount;
        this.categoryCount = categoryCount;
        this.fixedCount = fixedCount;
        this.occurrenceCount = occurrenceCount;
        this.settingCount = settingCount;
        this.exportedAt = exportedAt;
    }
}
