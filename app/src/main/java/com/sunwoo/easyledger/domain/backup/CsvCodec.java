package com.sunwoo.easyledger.domain.backup;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class CsvCodec {
    public static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private CsvCodec() {
    }

    public static byte[] write(String[] header, List<String[]> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append(line(header)).append('\n');
        for (String[] row : rows) {
            builder.append(line(row)).append('\n');
        }
        byte[] body = builder.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, out, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, out, UTF8_BOM.length, body.length);
        return out;
    }

    public static List<String[]> parse(byte[] data) throws BackupFormatException {
        String text = new String(data, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        List<String[]> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (ch == '\n') {
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row.toArray(new String[0]));
                row = new ArrayList<>();
            } else if (ch != '\r') {
                cell.append(ch);
            }
        }
        if (inQuotes) {
            throw new BackupFormatException("CSV 형식이 올바르지 않습니다.");
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row.toArray(new String[0]));
        }
        return rows;
    }

    private static String line(String[] cells) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            String value = cells[i] == null ? "" : cells[i];
            builder.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        return builder.toString();
    }
}
