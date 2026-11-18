package com.sunwoo.easyledger.domain.backup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class BackupArchive {
    private BackupArchive() {
    }

    public static byte[] zip(Map<String, byte[]> files) throws BackupFormatException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(buffer);
            for (String name : BackupFiles.REQUIRED) {
                byte[] body = files.get(name);
                if (body == null) {
                    throw new BackupFormatException("백업 파일이 빠졌습니다.");
                }
                BackupFiles.assertSafeEntryName(name);
                zip.putNextEntry(new ZipEntry(name));
                zip.write(body);
                zip.closeEntry();
            }
            zip.finish();
            zip.close();
            return buffer.toByteArray();
        } catch (IOException exception) {
            throw new BackupFormatException("백업 파일을 만들지 못했습니다.");
        }
    }

    public static Map<String, byte[]> unzip(byte[] zipBytes) throws BackupFormatException {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new BackupFormatException("백업 파일이 비어 있습니다.");
        }
        if (zipBytes.length > BackupFiles.MAX_ZIP_BYTES) {
            throw new BackupFormatException("백업 파일이 너무 큽니다.");
        }
        Map<String, byte[]> files = new LinkedHashMap<>();
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                BackupFiles.assertSafeEntryName(entry.getName());
                if (files.containsKey(entry.getName())) {
                    throw new BackupFormatException("백업 파일에 같은 이름이 반복됩니다.");
                }
                byte[] body = readEntry(zip);
                total += body.length;
                if (total > BackupFiles.MAX_TOTAL_UNCOMPRESSED) {
                    throw new BackupFormatException("백업 내용이 너무 큽니다.");
                }
                files.put(entry.getName(), body);
                zip.closeEntry();
            }
        } catch (BackupFormatException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BackupFormatException("백업 파일을 열 수 없습니다.");
        }
        for (String name : BackupFiles.REQUIRED) {
            if (!files.containsKey(name)) {
                throw new BackupFormatException("필수 파일이 없습니다.");
            }
        }
        if (files.size() != BackupFiles.REQUIRED.length) {
            throw new BackupFormatException("백업 파일 구성이 올바르지 않습니다.");
        }
        return files;
    }

    private static byte[] readEntry(ZipInputStream zip) throws IOException, BackupFormatException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = zip.read(chunk)) >= 0) {
            if (buffer.size() + read > BackupFiles.MAX_ENTRY_BYTES) {
                throw new BackupFormatException("백업 내용이 너무 큽니다.");
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }
}
