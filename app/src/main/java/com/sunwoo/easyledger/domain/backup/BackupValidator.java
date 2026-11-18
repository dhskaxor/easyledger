package com.sunwoo.easyledger.domain.backup;

import com.sunwoo.easyledger.domain.calculator.Money;
import com.sunwoo.easyledger.domain.model.Limits;
import com.sunwoo.easyledger.domain.model.OccurrenceStatus;
import com.sunwoo.easyledger.domain.model.PaymentMethod;
import com.sunwoo.easyledger.domain.model.RecurrenceType;
import com.sunwoo.easyledger.domain.model.TransactionRules;
import com.sunwoo.easyledger.domain.model.TransactionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BackupValidator {
    private BackupValidator() {
    }

    public static ValidatedBackup validate(Map<String, byte[]> files) throws BackupFormatException {
        List<String[]> infoRows = body(files.get(BackupFiles.BACKUP_INFO), BackupFiles.INFO_COLUMNS, BackupFiles.BACKUP_INFO);
        Map<String, String> info = new HashMap<>();
        for (String[] row : infoRows) {
            if (row[0].isEmpty() || info.containsKey(row[0])) {
                throw new BackupFormatException("백업 정보가 올바르지 않습니다.");
            }
            info.put(row[0], row[1]);
        }
        if (!String.valueOf(BackupFiles.FORMAT_VERSION).equals(info.get("format_version"))
                || !"EasyLedger".equals(info.get("app_name"))) {
            throw new BackupFormatException("이 앱에서 만든 백업 파일이 아닙니다.");
        }
        String schema = info.get("schema_version");
        if (!"1".equals(schema) && !String.valueOf(BackupFiles.SCHEMA_VERSION).equals(schema)) {
            throw new BackupFormatException("이 앱에서 만든 백업 파일이 아닙니다.");
        }
        parseEpoch(info.get("exported_at"));

        List<String[]> categories = categoriesBody(files.get(BackupFiles.CATEGORIES));
        List<String[]> transactions = body(files.get(BackupFiles.TRANSACTIONS), BackupFiles.TRANSACTION_COLUMNS, BackupFiles.TRANSACTIONS);
        List<String[]> fixed = body(files.get(BackupFiles.FIXED_TRANSACTIONS), BackupFiles.FIXED_COLUMNS, BackupFiles.FIXED_TRANSACTIONS);
        List<String[]> occurrences = body(files.get(BackupFiles.FIXED_OCCURRENCES), BackupFiles.OCCURRENCE_COLUMNS, BackupFiles.FIXED_OCCURRENCES);
        List<String[]> settings = body(files.get(BackupFiles.SETTINGS), BackupFiles.SETTING_COLUMNS, BackupFiles.SETTINGS);

        Map<String, String[]> categoryById = new HashMap<>();
        for (String[] row : categories) {
            requireId(row[0], BackupFiles.CATEGORIES);
            if (categoryById.put(row[0], row) != null) {
                throw new BackupFormatException("카테고리 ID가 중복되었습니다.");
            }
            storageType(row[2]);
            requireText(row[1], Limits.CATEGORY_NAME, "카테고리 이름");
            requireText(row[4], 40, "카테고리 아이콘");
            parseInt(row[5], "카테고리 순서");
            parseBoolean(row[6]);
            parseBoolean(row[7]);
            parseNonNegativeAmount(row[8], "자주 쓰는 금액1");
            parseNonNegativeAmount(row[9], "자주 쓰는 금액2");
            parseNonNegativeAmount(row[10], "자주 쓰는 금액3");
            parseEpoch(row[11]);
            parseEpoch(row[12]);
        }
        assertCategoryParents(categories);

        Set<String> fixedIds = new HashSet<>();
        for (String[] row : fixed) {
            requireId(row[0], BackupFiles.FIXED_TRANSACTIONS);
            if (!fixedIds.add(row[0])) {
                throw new BackupFormatException("반복 거래 ID가 중복되었습니다.");
            }
            String type = storageType(row[1]);
            requireText(row[2], Limits.FIXED_NAME, "반복 거래 이름");
            requirePositive(row[3]);
            assertCategoryType(categoryById, row[4], type);
            storagePayment(row[5]);
            RecurrenceType recurrence = storageRecurrence(row[6]);
            if (!TransactionRules.isDate(row[8]) || !TransactionRules.isDate(row[11])) {
                throw new BackupFormatException("반복 거래 날짜가 올바르지 않습니다.");
            }
            if (!row[9].isEmpty() && !TransactionRules.isDate(row[9])) {
                throw new BackupFormatException("반복 거래 종료일이 올바르지 않습니다.");
            }
            if (!row[9].isEmpty() && row[9].compareTo(row[8]) < 0) {
                throw new BackupFormatException("반복 거래 종료일이 시작일보다 빠릅니다.");
            }
            try {
                com.sunwoo.easyledger.domain.recurrence.RecurrenceCalendar.dates(
                        recurrence, row[7], java.time.LocalDate.parse(row[8]),
                        java.time.LocalDate.parse(row[8]), java.time.LocalDate.parse(row[8]));
            } catch (RuntimeException exception) {
                throw new BackupFormatException("반복 거래 규칙이 올바르지 않습니다.");
            }
            parseBoolean(row[10]);
            int version = parseInt(row[12], "규칙 버전");
            if (version < 1) {
                throw new BackupFormatException("규칙 버전이 올바르지 않습니다.");
            }
            parseEpoch(row[13]);
            parseEpoch(row[14]);
        }

        Map<String, String[]> transactionById = new HashMap<>();
        Set<String> occurrenceKeys = new HashSet<>();
        for (String[] row : transactions) {
            requireId(row[0], BackupFiles.TRANSACTIONS);
            if (transactionById.containsKey(row[0])) {
                throw new BackupFormatException("거래 ID가 중복되었습니다.");
            }
            transactionById.put(row[0], row);
            String type = storageType(row[1]);
            requirePositive(row[2]);
            if (!TransactionRules.isDate(row[3]) || !TransactionRules.isTime(row[4])) {
                throw new BackupFormatException("거래 날짜 또는 시간이 올바르지 않습니다.");
            }
            assertCategoryType(categoryById, row[5], type);
            storagePayment(row[6]);
            if (row[7].length() > Limits.MEMO) {
                throw new BackupFormatException("메모가 너무 깁니다.");
            }
            boolean fixedFlag = parseBoolean(row[10]);
            if (fixedFlag) {
                if (row[8].isEmpty() || row[9].isEmpty() || !fixedIds.contains(row[8])) {
                    throw new BackupFormatException("고정 거래 연결이 올바르지 않습니다.");
                }
                if (!occurrenceKeys.add(row[9])) {
                    throw new BackupFormatException("반복 발생 키가 중복되었습니다.");
                }
            } else if (!row[8].isEmpty() || !row[9].isEmpty()) {
                throw new BackupFormatException("일반 거래에 반복 정보가 들어 있습니다.");
            }
            parseEpoch(row[11]);
            parseEpoch(row[12]);
        }

        Set<String> occurrencePairs = new HashSet<>();
        Set<String> occurrenceIds = new HashSet<>();
        for (String[] row : occurrences) {
            requireId(row[0], BackupFiles.FIXED_OCCURRENCES);
            if (!occurrenceIds.add(row[0])) {
                throw new BackupFormatException("발생 이력 ID가 중복되었습니다.");
            }
            if (!fixedIds.contains(row[1])) {
                throw new BackupFormatException("발생 이력이 없는 반복 거래를 가리킵니다.");
            }
            if (!TransactionRules.isDate(row[2])) {
                throw new BackupFormatException("발생 예정일이 올바르지 않습니다.");
            }
            String pair = row[1] + "|" + row[2];
            if (!occurrencePairs.add(pair)) {
                throw new BackupFormatException("같은 반복 거래와 예정일이 중복되었습니다.");
            }
            OccurrenceStatus status = storageStatus(row[4]);
            if (!row[3].isEmpty() && !transactionById.containsKey(row[3])) {
                throw new BackupFormatException("발생 이력이 없는 거래를 가리킵니다.");
            }
            if (status == OccurrenceStatus.GENERATED && row[3].isEmpty()) {
                throw new BackupFormatException("생성된 발생 이력에 거래가 없습니다.");
            }
            if (!row[3].isEmpty()) {
                String[] transaction = transactionById.get(row[3]);
                if (!row[1].equals(transaction[8])) {
                    throw new BackupFormatException("발생 이력과 거래의 반복 설정이 다릅니다.");
                }
            }
            int version = parseInt(row[5], "발생 규칙 버전");
            if (version < 1) {
                throw new BackupFormatException("발생 규칙 버전이 올바르지 않습니다.");
            }
            parseEpoch(row[6]);
        }

        Set<String> settingKeys = new HashSet<>();
        for (String[] row : settings) {
            if (row[0].isEmpty() || row[0].length() > 80) {
                throw new BackupFormatException("설정 키가 올바르지 않습니다.");
            }
            if (!settingKeys.add(row[0])) {
                throw new BackupFormatException("설정 키가 중복되었습니다.");
            }
            if (row[1].length() > 500) {
                throw new BackupFormatException("설정값이 너무 깁니다.");
            }
            parseEpoch(row[2]);
        }

        BackupPreview preview = new BackupPreview(
                transactions.size(),
                categories.size(),
                fixed.size(),
                occurrences.size(),
                settings.size(),
                info.get("exported_at")
        );
        return new ValidatedBackup(categories, transactions, fixed, occurrences, settings, preview);
    }

    private static String storageType(String raw) throws BackupFormatException {
        try {
            return TransactionType.fromStorage(raw).name();
        } catch (RuntimeException exception) {
            throw new BackupFormatException("거래 유형이 올바르지 않습니다.");
        }
    }

    private static void storagePayment(String raw) throws BackupFormatException {
        try {
            PaymentMethod.fromStorage(raw);
        } catch (RuntimeException exception) {
            throw new BackupFormatException("결제수단이 올바르지 않습니다.");
        }
    }

    private static RecurrenceType storageRecurrence(String raw) throws BackupFormatException {
        try {
            return RecurrenceType.fromStorage(raw);
        } catch (RuntimeException exception) {
            throw new BackupFormatException("반복 주기가 올바르지 않습니다.");
        }
    }

    private static OccurrenceStatus storageStatus(String raw) throws BackupFormatException {
        try {
            return OccurrenceStatus.fromStorage(raw);
        } catch (RuntimeException exception) {
            throw new BackupFormatException("발생 상태가 올바르지 않습니다.");
        }
    }

    private static void assertCategoryParents(List<String[]> categories) throws BackupFormatException {
        Map<String, String[]> byId = new HashMap<>();
        for (String[] row : categories) {
            byId.put(row[0], row);
        }
        for (String[] row : categories) {
            if (row[3].isEmpty()) {
                continue;
            }
            String[] parent = byId.get(row[3]);
            if (parent == null) {
                throw new BackupFormatException("상위 카테고리를 찾을 수 없습니다.");
            }
            if (!parent[2].equals(row[2])) {
                throw new BackupFormatException("하위 카테고리의 유형이 상위와 다릅니다.");
            }
            if (row[0].equals(row[3])) {
                throw new BackupFormatException("카테고리 상위 연결이 올바르지 않습니다.");
            }
        }
        for (String[] row : categories) {
            Set<String> seen = new HashSet<>();
            String cursor = row[0];
            while (cursor != null && !cursor.isEmpty()) {
                if (!seen.add(cursor)) {
                    throw new BackupFormatException("카테고리 상위 연결이 순환합니다.");
                }
                String[] current = byId.get(cursor);
                cursor = current == null ? "" : current[3];
            }
        }
    }

    private static void assertCategoryType(Map<String, String[]> categories, String categoryId, String type)
            throws BackupFormatException {
        String[] category = categories.get(categoryId);
        if (category == null || !type.equals(category[2])) {
            throw new BackupFormatException("거래의 카테고리를 찾을 수 없습니다.");
        }
    }

    private static List<String[]> categoriesBody(byte[] data) throws BackupFormatException {
        if (data == null) {
            throw new BackupFormatException("필수 파일이 없습니다.");
        }
        List<String[]> parsed;
        try {
            parsed = CsvCodec.parse(data);
        } catch (BackupFormatException exception) {
            throw new BackupFormatException(BackupFiles.CATEGORIES + " 형식이 올바르지 않습니다.");
        }
        if (parsed.isEmpty()) {
            throw new BackupFormatException(BackupFiles.CATEGORIES + " 열이 맞지 않습니다.");
        }
        String[] header = parsed.get(0);
        boolean v1 = Arrays.equals(header, BackupFiles.CATEGORY_COLUMNS_V1);
        boolean v2 = Arrays.equals(header, BackupFiles.CATEGORY_COLUMNS);
        if (!v1 && !v2) {
            throw new BackupFormatException(BackupFiles.CATEGORIES + " 열이 맞지 않습니다.");
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i < parsed.size(); i++) {
            String[] row = parsed.get(i);
            if (row.length == 1 && row[0].isEmpty()) {
                continue;
            }
            if (row.length != header.length) {
                throw new BackupFormatException(BackupFiles.CATEGORIES + " " + (i + 1) + "행의 열 개수가 맞지 않습니다.");
            }
            rows.add(v1 ? upgradeCategoryRow(row) : row);
        }
        return rows;
    }

    private static String[] upgradeCategoryRow(String[] v1) {
        return new String[] {
                v1[0], v1[1], v1[2], v1[3], v1[4], v1[5], v1[6], v1[7],
                "0", "0", "0",
                v1[8], v1[9]
        };
    }

    private static List<String[]> body(byte[] data, String[] header, String file) throws BackupFormatException {
        if (data == null) {
            throw new BackupFormatException("필수 파일이 없습니다.");
        }
        List<String[]> parsed;
        try {
            parsed = CsvCodec.parse(data);
        } catch (BackupFormatException exception) {
            throw new BackupFormatException(file + " 형식이 올바르지 않습니다.");
        }
        if (parsed.isEmpty() || !Arrays.equals(parsed.get(0), header)) {
            throw new BackupFormatException(file + " 열이 맞지 않습니다.");
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i < parsed.size(); i++) {
            String[] row = parsed.get(i);
            if (row.length == 1 && row[0].isEmpty()) {
                continue;
            }
            if (row.length != header.length) {
                throw new BackupFormatException(file + " " + (i + 1) + "행의 열 개수가 맞지 않습니다.");
            }
            rows.add(row);
        }
        return rows;
    }

    private static void requireId(String id, String file) throws BackupFormatException {
        if (id == null || id.trim().isEmpty() || id.length() > 80 || id.indexOf('|') >= 0) {
            throw new BackupFormatException(file + " ID가 올바르지 않습니다.");
        }
    }

    private static void requireText(String value, int max, String label) throws BackupFormatException {
        if (value == null || value.trim().isEmpty() || value.trim().length() > max) {
            throw new BackupFormatException(label + "이 올바르지 않습니다.");
        }
    }

    private static void requirePositive(String raw) throws BackupFormatException {
        long amount;
        try {
            amount = Money.parsePlain(raw);
        } catch (RuntimeException exception) {
            throw new BackupFormatException("금액이 올바르지 않습니다.");
        }
        if (amount <= 0) {
            throw new BackupFormatException("금액은 0보다 커야 합니다.");
        }
    }

    private static void parseNonNegativeAmount(String raw, String label) throws BackupFormatException {
        long amount;
        try {
            amount = Money.parsePlain(raw);
        } catch (RuntimeException exception) {
            throw new BackupFormatException(label + "이 올바르지 않습니다.");
        }
        if (amount < 0) {
            throw new BackupFormatException(label + "이 올바르지 않습니다.");
        }
    }

    private static int parseInt(String raw, String label) throws BackupFormatException {
        try {
            return Integer.parseInt(raw);
        } catch (RuntimeException exception) {
            throw new BackupFormatException(label + "이 올바르지 않습니다.");
        }
    }

    private static long parseEpoch(String raw) throws BackupFormatException {
        try {
            long value = Long.parseLong(raw);
            if (value < 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (RuntimeException exception) {
            throw new BackupFormatException("시각 값이 올바르지 않습니다.");
        }
    }

    public static boolean parseBoolean(String raw) throws BackupFormatException {
        if ("true".equals(raw) || "1".equals(raw)) {
            return true;
        }
        if ("false".equals(raw) || "0".equals(raw)) {
            return false;
        }
        throw new BackupFormatException("참/거짓 값이 올바르지 않습니다.");
    }
}
