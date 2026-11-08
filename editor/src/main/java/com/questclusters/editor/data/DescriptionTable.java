package com.questclusters.editor.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * A table in a quest description: one line {@code {quest_enhance_table:<payload>}}, payload = base64url (no padding)
 * of zlib-compressed UTF-8 JSON, format version 3 (the format Quest Enhance books use). Mutable, for the editor.
 */
public final class DescriptionTable {
    public static final String KEY = "quest_enhance_table";
    public static final int FORMAT = 3, MAX_COLUMNS = 8, MAX_ROWS = 16, MAX_CELL = 128;
    public static final int LEFT = 0, CENTER = 1, RIGHT = 2;

    public record Merge(int row, int column, int rowSpan, int columnSpan) {
        public boolean contains(int r, int c) {
            return r >= row && r < row + rowSpan && c >= column && c < column + columnSpan;
        }

        boolean overlaps(Merge o) {
            return row < o.row + o.rowSpan && o.row < row + rowSpan && column < o.column + o.columnSpan && o.column < column + columnSpan;
        }
    }

    public record Style(boolean header, int alignment, int cellWidth, int rowHeight, int lineWidth,
                        int borderColor, int headerColor, int cellColor, int textColor) {
    }

    public int columns = 3;
    public boolean header = true;
    public int alignment = CENTER;
    public int tableWidth = 0, rowHeight = 18, lineWidth = 1;
    public int borderColor = 0xFF999999, headerColor = 0xFF374151, cellColor = 0xFF1F2937, textColor = 0xFFFFFFFF;
    public final List<List<String>> rows = new ArrayList<>();
    public final List<Merge> merges = new ArrayList<>();
    /** Sparse: "row,column" → style; missing cells use {@link #defaultStyle}. */
    public final Map<String, Style> cellStyles = new LinkedHashMap<>();

    public static DescriptionTable createDefault(List<List<String>> rows) {
        DescriptionTable t = new DescriptionTable();
        t.columns = rows.getFirst().size();
        rows.forEach(r -> t.rows.add(new ArrayList<>(r)));
        return t;
    }

    public DescriptionTable copy() {
        DescriptionTable t = decode(encode());
        return t == null ? new DescriptionTable() : t;
    }

    public Style defaultStyle(int row) {
        return new Style(header && row == 0, alignment, 0, rowHeight, lineWidth, borderColor, headerColor, cellColor, textColor);
    }

    public Style style(int row, int column) {
        Style s = cellStyles.get(row + "," + column);
        return s != null ? s : defaultStyle(row);
    }

    public void setStyle(int row, int column, Style s) {
        if (s.equals(defaultStyle(row))) cellStyles.remove(row + "," + column);
        else cellStyles.put(row + "," + column, s);
    }

    public String cell(int row, int column) {
        List<String> r = rows.get(row);
        return column < r.size() ? r.get(column) : "";
    }

    public void setCell(int row, int column, String text) {
        List<String> r = rows.get(row);
        while (r.size() <= column) r.add("");
        r.set(column, text.length() > MAX_CELL ? text.substring(0, MAX_CELL) : text);
    }

    /** The merge covering a cell, if any. */
    public Merge mergeAt(int row, int column) {
        for (Merge m : merges) if (m.contains(row, column)) return m;
        return null;
    }

    /** False for cells hidden under another cell's merge. */
    public boolean isAnchor(int row, int column) {
        Merge m = mergeAt(row, column);
        return m == null || m.row() == row && m.column() == column;
    }

    // ------------------------------------------------------------------ structure edits

    public boolean merge(int r0, int c0, int r1, int c1) {
        Merge m = new Merge(Math.min(r0, r1), Math.min(c0, c1), Math.abs(r1 - r0) + 1, Math.abs(c1 - c0) + 1);
        if (m.rowSpan() == 1 && m.columnSpan() == 1) return false;
        merges.removeIf(m::overlaps);
        merges.add(m);
        return true;
    }

    public boolean split(int r0, int c0, int r1, int c1) {
        Merge sel = new Merge(Math.min(r0, r1), Math.min(c0, c1), Math.abs(r1 - r0) + 1, Math.abs(c1 - c0) + 1);
        return merges.removeIf(sel::overlaps);
    }

    public boolean insertRow(int at) {
        if (rows.size() >= MAX_ROWS) return false;
        List<String> r = new ArrayList<>();
        for (int i = 0; i < columns; i++) r.add("");
        rows.add(at, r);
        shift(true, at, 1);
        return true;
    }

    public boolean removeRow(int at) {
        if (rows.size() <= 1) return false;
        rows.remove(at);
        shift(true, at, -1);
        return true;
    }

    public boolean insertColumn(int at) {
        if (columns >= MAX_COLUMNS) return false;
        columns++;
        for (List<String> r : rows) {
            while (r.size() < columns - 1) r.add("");
            r.add(Math.min(at, r.size()), "");
        }
        shift(false, at, 1);
        return true;
    }

    public boolean removeColumn(int at) {
        if (columns <= 1) return false;
        columns--;
        for (List<String> r : rows) if (at < r.size()) r.remove(at);
        shift(false, at, -1);
        return true;
    }

    /** Moves merges and styles after an inserted (+1) or removed (-1) row/column at index {@code at}. */
    private void shift(boolean row, int at, int delta) {
        List<Merge> out = new ArrayList<>();
        for (Merge m : merges) {
            int start = row ? m.row() : m.column(), span = row ? m.rowSpan() : m.columnSpan();
            if (delta > 0) {
                if (at <= start) start++;
                else if (at < start + span) span++;
            } else {
                if (at < start) start--;
                else if (at < start + span) span--;
            }
            Merge n = row ? new Merge(start, m.column(), span, m.columnSpan()) : new Merge(m.row(), start, m.rowSpan(), span);
            if (span > 0 && (n.rowSpan() > 1 || n.columnSpan() > 1)) out.add(n);
        }
        merges.clear();
        merges.addAll(out);
        Map<String, Style> styles = new LinkedHashMap<>();
        cellStyles.forEach((k, s) -> {
            int r = Integer.parseInt(k.substring(0, k.indexOf(','))), c = Integer.parseInt(k.substring(k.indexOf(',') + 1));
            int idx = row ? r : c;
            if (delta < 0 && idx == at) return;
            if (idx >= at) idx += delta;
            styles.put(row ? idx + "," + c : r + "," + idx, s);
        });
        cellStyles.clear();
        cellStyles.putAll(styles);
    }

    // ------------------------------------------------------------------ layout

    /** Column widths in px for the given available width (Quest Enhance's algorithm, so tables keep their look). */
    public int[] columnWidths(int available) {
        int total = tableWidth > 0 ? Math.min(available, tableWidth) : available;
        total = Math.max(total, columns);
        int[] req = new int[columns];
        for (int r = 0; r < rows.size(); r++) {
            for (int c = 0; c < columns; c++) req[c] = Math.max(req[c], style(r, c).cellWidth());
        }
        int fixed = 0, auto = 0;
        for (int w : req) {
            if (w > 0) fixed += w;
            else auto++;
        }
        int[] out = new int[columns];
        if (auto > 0 && fixed + auto <= total) {
            int rest = total - fixed, each = rest / auto, extra = rest - each * auto;
            for (int c = 0; c < columns; c++) {
                if (req[c] > 0) out[c] = req[c];
                else {
                    out[c] = each + (extra > 0 ? 1 : 0);
                    if (extra > 0) extra--;
                }
            }
        } else if (auto == 0 && fixed <= total) {
            System.arraycopy(req, 0, out, 0, columns);
            out[columns - 1] += total - fixed;
        } else {
            double weight = 0;
            for (int w : req) weight += w > 0 ? w : 1;
            int sum = 0;
            for (int c = 0; c < columns; c++) {
                out[c] = Math.max(1, (int) Math.floor((req[c] > 0 ? req[c] : 1) * total / weight));
                sum += out[c];
            }
            for (int c = columns - 1; sum != total && c >= 0; c--) {
                int d = sum < total ? 1 : (out[c] > 1 ? -1 : 0);
                out[c] += d;
                sum += d;
                if (c == 0 && sum != total) c = columns;
                if (d == 0 && c == 0) break;
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ markup

    public String toLine() {
        return "{" + KEY + ":" + encode() + "}";
    }

    /** Payload of a description line, or null if it is not a table line. */
    public static String payloadOf(String line) {
        String s = line == null ? "" : line.trim();
        String prefix = "{" + KEY + ":";
        return s.startsWith(prefix) && s.endsWith("}") ? s.substring(prefix.length(), s.length() - 1).trim() : null;
    }

    public String encode() {
        JsonObject o = new JsonObject();
        o.addProperty("format_version", FORMAT);
        o.addProperty("columns", columns);
        o.addProperty("header", header);
        o.addProperty("alignment", alignName(alignment));
        o.addProperty("table_width", tableWidth);
        o.addProperty("row_height", rowHeight);
        o.addProperty("line_width", lineWidth);
        o.addProperty("border_color", borderColor);
        o.addProperty("header_color", headerColor);
        o.addProperty("cell_color", cellColor);
        o.addProperty("text_color", textColor);
        JsonArray rs = new JsonArray();
        for (List<String> r : rows) {
            JsonArray ra = new JsonArray();
            for (int c = 0; c < columns; c++) ra.add(c < r.size() ? r.get(c) : "");
            rs.add(ra);
        }
        o.add("rows", rs);
        JsonArray ms = new JsonArray();
        for (Merge m : merges) {
            JsonObject mo = new JsonObject();
            mo.addProperty("row", m.row());
            mo.addProperty("column", m.column());
            mo.addProperty("row_span", m.rowSpan());
            mo.addProperty("column_span", m.columnSpan());
            ms.add(mo);
        }
        o.add("merges", ms);
        JsonArray ss = new JsonArray();
        cellStyles.forEach((k, s) -> {
            JsonObject so = new JsonObject();
            so.addProperty("row", Integer.parseInt(k.substring(0, k.indexOf(','))));
            so.addProperty("column", Integer.parseInt(k.substring(k.indexOf(',') + 1)));
            so.addProperty("header", s.header());
            so.addProperty("alignment", alignName(s.alignment()));
            so.addProperty("cell_width", s.cellWidth());
            so.addProperty("row_height", s.rowHeight());
            so.addProperty("line_width", s.lineWidth());
            so.addProperty("border_color", s.borderColor());
            so.addProperty("header_color", s.headerColor());
            so.addProperty("cell_color", s.cellColor());
            so.addProperty("text_color", s.textColor());
            ss.add(so);
        });
        o.add("cell_styles", ss);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DeflaterOutputStream out = new DeflaterOutputStream(bytes, new Deflater(Deflater.BEST_COMPRESSION))) {
                out.write(o.toString().getBytes(StandardCharsets.UTF_8));
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Null if the payload is not a valid version-3 table. */
    public static DescriptionTable decode(String payload) {
        try {
            byte[] raw = Base64.getUrlDecoder().decode(payload.trim());
            String json;
            try (InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(raw))) {
                json = new String(in.readNBytes(1 << 20), StandardCharsets.UTF_8);
            }
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (o.get("format_version").getAsInt() != FORMAT) return null;
            DescriptionTable t = new DescriptionTable();
            t.columns = clamp(o.get("columns").getAsInt(), 1, MAX_COLUMNS);
            t.header = bool(o, "header", true);
            t.alignment = align(str(o, "alignment"));
            t.tableWidth = clamp(integer(o, "table_width", 0), 0, 1000);
            t.rowHeight = clamp(integer(o, "row_height", 18), 12, 30);
            t.lineWidth = clamp(integer(o, "line_width", 1), 1, 4);
            t.borderColor = integer(o, "border_color", t.borderColor);
            t.headerColor = integer(o, "header_color", t.headerColor);
            t.cellColor = integer(o, "cell_color", t.cellColor);
            t.textColor = integer(o, "text_color", t.textColor);
            JsonArray rs = o.getAsJsonArray("rows");
            for (int i = 0; i < rs.size() && i < MAX_ROWS; i++) {
                List<String> r = new ArrayList<>();
                JsonArray ra = rs.get(i).getAsJsonArray();
                for (int c = 0; c < t.columns; c++) {
                    String v = c < ra.size() && !ra.get(c).isJsonNull() ? ra.get(c).getAsString() : "";
                    r.add(v.length() > MAX_CELL ? v.substring(0, MAX_CELL) : v);
                }
                t.rows.add(r);
            }
            if (t.rows.isEmpty()) return null;
            if (o.has("merges")) {
                for (JsonElement e : o.getAsJsonArray("merges")) {
                    JsonObject m = e.getAsJsonObject();
                    Merge mg = new Merge(integer(m, "row", 0), integer(m, "column", 0), integer(m, "row_span", 1), integer(m, "column_span", 1));
                    boolean inside = mg.row() >= 0 && mg.column() >= 0 && mg.rowSpan() >= 1 && mg.columnSpan() >= 1
                            && mg.row() + mg.rowSpan() <= t.rows.size() && mg.column() + mg.columnSpan() <= t.columns;
                    if (inside && (mg.rowSpan() > 1 || mg.columnSpan() > 1) && t.merges.stream().noneMatch(mg::overlaps)) {
                        t.merges.add(mg);
                    }
                }
            }
            if (o.has("cell_styles")) {
                for (JsonElement e : o.getAsJsonArray("cell_styles")) {
                    JsonObject s = e.getAsJsonObject();
                    int r = integer(s, "row", -1), c = integer(s, "column", -1);
                    if (r < 0 || c < 0 || r >= t.rows.size() || c >= t.columns) continue;
                    Style d = t.defaultStyle(r);
                    t.cellStyles.put(r + "," + c, new Style(bool(s, "header", d.header()),
                            s.has("alignment") ? align(str(s, "alignment")) : d.alignment(),
                            clamp(integer(s, "cell_width", 0), 0, 1000), clamp(integer(s, "row_height", d.rowHeight()), 12, 30),
                            clamp(integer(s, "line_width", d.lineWidth()), 1, 4), integer(s, "border_color", d.borderColor()),
                            integer(s, "header_color", d.headerColor()), integer(s, "cell_color", d.cellColor()),
                            integer(s, "text_color", d.textColor())));
                }
            }
            return t;
        } catch (Exception e) {
            return null;
        }
    }

    public static String alignName(int a) {
        return a == LEFT ? "left" : a == RIGHT ? "right" : "center";
    }

    public static int align(String s) {
        return "left".equals(s) ? LEFT : "right".equals(s) ? RIGHT : CENTER;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int integer(JsonObject o, String k, int d) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsInt() : d;
    }

    private static boolean bool(JsonObject o, String k, boolean d) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsBoolean() : d;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "";
    }
}
