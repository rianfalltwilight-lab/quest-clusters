package com.questclusters.editor.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatchTest {
    @Test
    void applyAndUndo() {
        BookExtras b = new BookExtras();
        b.editChapter(1).background = "a:old.png";
        b.setQuest(5, new QuestExtras("a:box.png", "", false));

        EditorPatch p = new EditorPatch();
        p.sidebar = "a:side.png";
        ChapterExtras c = b.chapter(1).copy();
        c.background = "";
        c.decorativeLines.add(List.of("q:1", "q:2"));
        p.chapters.put(1L, c);
        p.quests.put(5L, QuestExtras.EMPTY);
        p.quests.put(6L, new QuestExtras("", "a:view.png", true));
        p.images.put(9L, true);

        EditorPatch wire = EditorPatch.read(p.write());
        assertNotNull(wire);
        EditorPatch undo = wire.snapshot(b);
        wire.applyTo(b);
        assertEquals("a:side.png", b.sidebarBackground);
        assertEquals("", b.chapter(1).background);
        assertEquals(1, b.chapter(1).decorativeLines.size());
        assertTrue(b.quest(5).isEmpty());
        assertTrue(b.quest(6).disableScrollPaging());
        assertTrue(b.excludedImages.contains(9L));

        EditorPatch.read(undo.write()).applyTo(b);
        assertEquals("", b.sidebarBackground);
        assertEquals("a:old.png", b.chapter(1).background);
        assertTrue(b.chapter(1).decorativeLines.isEmpty());
        assertEquals("a:box.png", b.quest(5).boxBackground());
        assertTrue(b.quest(6).isEmpty());
        assertFalse(b.excludedImages.contains(9L));
    }

    @Test
    void rejectsBadValues() {
        EditorPatch p = new EditorPatch();
        p.quests.put(1L, new QuestExtras("NOT valid!", "", false));
        assertNull(EditorPatch.read(p.write()));
        EditorPatch s = new EditorPatch();
        s.sidebar = "C:\\evil";
        assertNull(EditorPatch.read(s.write()));
    }
}
