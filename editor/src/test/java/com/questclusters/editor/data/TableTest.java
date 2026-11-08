package com.questclusters.editor.data;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableTest {
    private static DescriptionTable sample() {
        return DescriptionTable.createDefault(List.of(List.of("A", "B", "C"), List.of("1", "2", "3"), List.of("4", "5", "6")));
    }

    @Test
    void roundTrip() {
        DescriptionTable t = sample();
        t.merge(1, 0, 2, 0);
        t.setStyle(0, 1, new DescriptionTable.Style(true, DescriptionTable.LEFT, 64, 20, 2, -1, -2, -3, -4));
        t.setCell(2, 2, "multi\nline");
        String line = t.toLine();
        assertTrue(line.startsWith("{quest_enhance_table:eN"));
        DescriptionTable back = DescriptionTable.decode(DescriptionTable.payloadOf(line));
        assertNotNull(back);
        assertEquals(t.encode(), back.encode());
        assertEquals("multi\nline", back.cell(2, 2));
        assertEquals(1, back.merges.size());
        assertFalse(back.isAnchor(2, 0));
        assertEquals(64, back.style(0, 1).cellWidth());
        assertTrue(back.style(0, 0).header());
        assertFalse(back.style(1, 0).header());
    }

    @Test
    void rejectsGarbage() {
        assertNull(DescriptionTable.decode("not base64!"));
        assertNull(DescriptionTable.payloadOf("plain text"));
    }

    @Test
    void structureEdits() {
        DescriptionTable t = sample();
        t.merge(0, 1, 1, 2); // rows 0-1, columns 1-2
        t.insertRow(1);
        assertEquals(new DescriptionTable.Merge(0, 1, 3, 2), t.merges.getFirst());
        t.removeColumn(1);
        assertEquals(new DescriptionTable.Merge(0, 1, 3, 1), t.merges.getFirst());
        t.removeRow(0);
        t.removeRow(0);
        assertTrue(t.merges.isEmpty()); // shrank to a single cell
        assertEquals(2, t.columns);
        assertTrue(t.split(0, 0, 0, 0) == false);
    }

    @Test
    void columnWidths() {
        DescriptionTable t = sample();
        assertArrayEquals(new int[]{34, 33, 33}, t.columnWidths(100));
        t.setStyle(0, 0, new DescriptionTable.Style(true, 1, 50, 18, 1, t.borderColor, t.headerColor, t.cellColor, t.textColor));
        assertArrayEquals(new int[]{50, 25, 25}, t.columnWidths(100));
        int[] tight = t.columnWidths(40);
        assertEquals(40, Arrays.stream(tight).sum());
        t.tableWidth = 60;
        assertEquals(60, Arrays.stream(t.columnWidths(100)).sum());
    }
}
