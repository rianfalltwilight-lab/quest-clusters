package com.questclusters.editor.client.desc;

import com.questclusters.editor.data.DescriptionTable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/** Lays out and draws a {@link DescriptionTable}; shared by the details view and the table editor. */
public final class TableRenderer {
    private final DescriptionTable table;
    int[] colX, rowY;
    private int width = -1;

    public TableRenderer(DescriptionTable table) {
        this.table = table;
    }

    /** Lays out for the given width; returns the total height. */
    public int layout(int width) {
        Font font = Minecraft.getInstance().font;
        int lw = table.lineWidth, rows = table.rows.size(), cols = table.columns;
        int[] colW = table.columnWidths(Math.max(cols, width - lw));
        colX = new int[cols + 1];
        for (int c = 0; c < cols; c++) colX[c + 1] = colX[c] + colW[c];
        int[] rowH = new int[rows];
        for (int r = 0; r < rows; r++) {
            int h = table.rowHeight;
            for (int c = 0; c < cols; c++) h = Math.max(h, table.style(r, c).rowHeight());
            rowH[r] = h;
        }
        for (int pass = 0; pass < 2; pass++) { // single-row cells first; merged regions then add to their first row
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (!table.isAnchor(r, c)) continue;
                    DescriptionTable.Merge m = table.mergeAt(r, c);
                    int rs = m == null ? 1 : m.rowSpan(), cs = m == null ? 1 : m.columnSpan();
                    if ((rs > 1) != (pass == 1)) continue;
                    int need = lines(font, table.cell(r, c), colX[c + cs] - colX[c] - 6 - lw).size() * font.lineHeight + 6;
                    int have = 0;
                    for (int i = r; i < r + rs; i++) have += rowH[i];
                    if (need > have) rowH[r] += need - have;
                }
            }
        }
        rowY = new int[rows + 1];
        for (int r = 0; r < rows; r++) rowY[r + 1] = rowY[r] + rowH[r];
        this.width = width;
        return lw + rowY[rows];
    }

    public int height() {
        return table.lineWidth + rowY[table.rows.size()];
    }

    public static List<FormattedCharSequence> lines(Font font, String text, int width) {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (String part : text.split("\n", -1)) {
            List<FormattedCharSequence> wrapped = font.split(Component.literal(part), Math.max(8, width));
            if (wrapped.isEmpty()) out.add(FormattedCharSequence.EMPTY);
            else out.addAll(wrapped);
        }
        return out;
    }

    /** Cell under (mx, my) relative to the table's top-left, as {row, column} of its anchor; null if outside. */
    public int[] cellAt(int mx, int my) {
        if (mx < 0 || my < 0) return null;
        for (int r = 0; r < table.rows.size(); r++) {
            if (my < rowY[r] || my >= rowY[r + 1] + (r == table.rows.size() - 1 ? table.lineWidth : 0)) continue;
            for (int c = 0; c < table.columns; c++) {
                if (mx >= colX[c] && mx < colX[c + 1] + (c == table.columns - 1 ? table.lineWidth : 0)) {
                    DescriptionTable.Merge m = table.mergeAt(r, c);
                    return m == null ? new int[]{r, c} : new int[]{m.row(), m.column()};
                }
            }
        }
        return null;
    }

    /** Rectangle {x0, y0, x1, y1} of a cell's region (merge-aware), relative to the table. */
    public int[] rect(int r, int c) {
        DescriptionTable.Merge m = table.mergeAt(r, c);
        int rs = m == null ? 1 : m.rowSpan(), cs = m == null ? 1 : m.columnSpan();
        int r0 = m == null ? r : m.row(), c0 = m == null ? c : m.column();
        return new int[]{colX[c0], rowY[r0], colX[c0 + cs] + table.lineWidth, rowY[r0 + rs] + table.lineWidth};
    }

    /**
     * @param textOverride if not null, returns the text to show for a cell (the editor's in-progress text), or null
     */
    public void draw(GuiGraphics g, int x, int y, int w, BiFunction<Integer, Integer, String> textOverride) {
        if (width != w) layout(w);
        Font font = Minecraft.getInstance().font;
        for (int r = 0; r < table.rows.size(); r++) {
            for (int c = 0; c < table.columns; c++) {
                if (!table.isAnchor(r, c)) continue;
                DescriptionTable.Style s = table.style(r, c);
                int[] rc = rect(r, c);
                int x0 = x + rc[0], y0 = y + rc[1], x1 = x + rc[2], y1 = y + rc[3], lw = s.lineWidth();
                g.fill(x0, y0, x1, y1, s.header() ? s.headerColor() : s.cellColor());
                g.fill(x0, y0, x1, y0 + lw, s.borderColor());
                g.fill(x0, y1 - lw, x1, y1, s.borderColor());
                g.fill(x0, y0, x0 + lw, y1, s.borderColor());
                g.fill(x1 - lw, y0, x1, y1, s.borderColor());
                String text = textOverride == null ? null : textOverride.apply(r, c);
                List<FormattedCharSequence> lines = lines(font, text != null ? text : table.cell(r, c), x1 - x0 - 6 - lw);
                int inset = 3 + lw, th = lines.size() * font.lineHeight;
                int ty = y0 + Math.max(2, (y1 - y0 - th) / 2) + 1;
                for (FormattedCharSequence line : lines) {
                    int tw = font.width(line);
                    int tx = switch (s.alignment()) {
                        case DescriptionTable.LEFT -> x0 + inset;
                        case DescriptionTable.RIGHT -> x1 - inset - tw;
                        default -> x0 + (x1 - x0 - tw) / 2;
                    };
                    g.drawString(font, line, tx, ty, s.textColor(), false);
                    ty += font.lineHeight;
                }
            }
        }
    }
}
