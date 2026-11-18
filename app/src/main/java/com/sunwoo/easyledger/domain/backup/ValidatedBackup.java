package com.sunwoo.easyledger.domain.backup;

import java.util.List;

public final class ValidatedBackup {
    public final List<String[]> categories;
    public final List<String[]> transactions;
    public final List<String[]> fixedTransactions;
    public final List<String[]> occurrences;
    public final List<String[]> settings;
    public final BackupPreview preview;

    public ValidatedBackup(
            List<String[]> categories,
            List<String[]> transactions,
            List<String[]> fixedTransactions,
            List<String[]> occurrences,
            List<String[]> settings,
            BackupPreview preview
    ) {
        this.categories = categories;
        this.transactions = transactions;
        this.fixedTransactions = fixedTransactions;
        this.occurrences = occurrences;
        this.settings = settings;
        this.preview = preview;
    }
}
