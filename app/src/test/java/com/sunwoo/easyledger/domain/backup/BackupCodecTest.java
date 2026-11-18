package com.sunwoo.easyledger.domain.backup;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class BackupCodecTest {
    @Test
    public void roundTripsKoreanMemoAndQuotedComma() throws Exception {
        byte[] csv = CsvCodec.write(
                new String[] {"name", "memo"},
                java.util.Collections.singletonList(new String[] {"식비", "점심, \"김밥\""})
        );
        assertEquals((byte) 0xEF, csv[0]);
        List<String[]> rows = CsvCodec.parse(csv);
        assertEquals("식비", rows.get(1)[0]);
        assertEquals("점심, \"김밥\"", rows.get(1)[1]);
    }

    @Test
    public void rejectsZipSlipMissingFileAndBadAmount() throws Exception {
        assertThrows(BackupFormatException.class, () -> BackupArchive.unzip(zipWith("../evil.csv", "x")));
        assertThrows(BackupFormatException.class, () -> BackupArchive.unzip(zipWith("notes.csv", "x")));

        Map<String, byte[]> files = validFilesV2();
        String broken = new String(files.get(BackupFiles.TRANSACTIONS), StandardCharsets.UTF_8)
                .replace("\"4500\"", "\"12.5\"");
        files.put(BackupFiles.TRANSACTIONS, withBom(broken));
        assertThrows(BackupFormatException.class, () -> BackupValidator.validate(files));
    }

    @Test
    public void acceptsConsistentBackupAndRejectsDuplicateOrBrokenReference() throws Exception {
        ValidatedBackup backup = BackupValidator.validate(validFilesV2());
        assertEquals(1, backup.preview.transactionCount);
        assertEquals(1, backup.preview.fixedCount);
        assertTrue(backup.preview.categoryCount >= 1);

        Map<String, byte[]> duplicate = validFilesV2();
        String text = new String(duplicate.get(BackupFiles.TRANSACTIONS), StandardCharsets.UTF_8);
        duplicate.put(BackupFiles.TRANSACTIONS, withBom(text + text.substring(text.indexOf('\n') + 1)));
        assertThrows(BackupFormatException.class, () -> BackupValidator.validate(duplicate));

        Map<String, byte[]> missingCategory = validFilesV2();
        String categories = new String(missingCategory.get(BackupFiles.CATEGORIES), StandardCharsets.UTF_8);
        missingCategory.put(BackupFiles.CATEGORIES, withBom(categories.replace("cat-food", "other")));
        assertThrows(BackupFormatException.class, () -> BackupValidator.validate(missingCategory));
    }

    @Test
    public void acceptsSchemaV1CategoriesWithoutQuickAmounts() throws Exception {
        ValidatedBackup backup = BackupValidator.validate(validFilesV1());
        assertEquals(1, backup.preview.categoryCount);
        assertEquals("0", backup.categories.get(0)[8]);
        assertEquals("0", backup.categories.get(0)[9]);
        assertEquals("0", backup.categories.get(0)[10]);
    }

    private static Map<String, byte[]> validFilesV2() {
        Map<String, byte[]> files = baseFiles();
        files.put(BackupFiles.CATEGORIES, CsvCodec.write(BackupFiles.CATEGORY_COLUMNS, java.util.Collections.singletonList(
                new String[] {
                        "cat-food", "식비", "EXPENSE", "", "food", "0", "false", "true",
                        "8000", "0", "0", "10", "10"
                }
        )));
        files.put(BackupFiles.BACKUP_INFO, CsvCodec.write(BackupFiles.INFO_COLUMNS, java.util.Arrays.asList(
                new String[] {"format_version", "1"},
                new String[] {"schema_version", "2"},
                new String[] {"app_name", "EasyLedger"},
                new String[] {"exported_at", "10"}
        )));
        return files;
    }

    private static Map<String, byte[]> validFilesV1() {
        Map<String, byte[]> files = baseFiles();
        files.put(BackupFiles.CATEGORIES, CsvCodec.write(BackupFiles.CATEGORY_COLUMNS_V1, java.util.Collections.singletonList(
                new String[] {"cat-food", "식비", "EXPENSE", "", "food", "0", "false", "true", "10", "10"}
        )));
        files.put(BackupFiles.BACKUP_INFO, CsvCodec.write(BackupFiles.INFO_COLUMNS, java.util.Arrays.asList(
                new String[] {"format_version", "1"},
                new String[] {"schema_version", "1"},
                new String[] {"app_name", "EasyLedger"},
                new String[] {"exported_at", "10"}
        )));
        return files;
    }

    private static Map<String, byte[]> baseFiles() {
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put(BackupFiles.TRANSACTIONS, CsvCodec.write(BackupFiles.TRANSACTION_COLUMNS, java.util.Collections.singletonList(
                new String[] {
                        "tx-1", "EXPENSE", "4500", "2026-11-16", "09:00:00", "cat-food", "CARD",
                        "점심,김밥", "fixed-1", "fixed-1|2026-11-16", "true", "10", "10"
                }
        )));
        files.put(BackupFiles.FIXED_TRANSACTIONS, CsvCodec.write(BackupFiles.FIXED_COLUMNS, java.util.Collections.singletonList(
                new String[] {
                        "fixed-1", "EXPENSE", "점심", "4500", "cat-food", "CARD", "MONTHLY", "16",
                        "2026-11-16", "", "true", "2026-11-16", "1", "10", "10"
                }
        )));
        files.put(BackupFiles.FIXED_OCCURRENCES, CsvCodec.write(BackupFiles.OCCURRENCE_COLUMNS, java.util.Collections.singletonList(
                new String[] {"occ-1", "fixed-1", "2026-11-16", "tx-1", "GENERATED", "1", "10"}
        )));
        files.put(BackupFiles.SETTINGS, CsvCodec.write(BackupFiles.SETTING_COLUMNS, java.util.Collections.singletonList(
                new String[] {"default_payment_method", "CARD", "10"}
        )));
        return files;
    }

    private static byte[] withBom(String text) {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[CsvCodec.UTF8_BOM.length + body.length];
        System.arraycopy(CsvCodec.UTF8_BOM, 0, out, 0, CsvCodec.UTF8_BOM.length);
        System.arraycopy(body, 0, out, CsvCodec.UTF8_BOM.length, body.length);
        return out;
    }

    private static byte[] zipWith(String name, String body) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(buffer);
        zip.putNextEntry(new ZipEntry(name));
        zip.write(body.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
        zip.close();
        return buffer.toByteArray();
    }
}
