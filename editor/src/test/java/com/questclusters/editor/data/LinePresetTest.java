package com.questclusters.editor.data;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LinePresetTest {
    private static final LinePreset CHAIN = new LinePreset("a1", "Chain", "minecraft:block/chain");
    private static final LinePreset ROPE = new LinePreset("b2", "Rope", "mymod:textures/gui/rope.png");

    @Test
    void bookAndChapterFilesRoundTrip() {
        BookExtras b = new BookExtras();
        b.linePresets.addAll(List.of(CHAIN, ROPE));
        ChapterExtras c = b.editChapter(7);
        c.linePreset = "a1";
        c.nodeLinePresets.put("q:00000000000000D5", "b2");

        BookExtras back = new BookExtras();
        ExtrasNbt.readBook(ExtrasNbt.writeBook(b, CompoundTag::new), back);
        assertEquals(List.of(CHAIN, ROPE), back.linePresets);
        ExtrasNbt.readChapter(ExtrasNbt.writeChapter(7, c, Map.of(), List.of(), CompoundTag::new), back);
        assertEquals("a1", back.chapter(7).linePreset);
        assertEquals("b2", back.chapter(7).presetFor("q:00000000000000D5"));
        assertEquals("a1", back.chapter(7).presetFor("q:0000000000000001"), "other nodes use the chapter default");
        assertEquals(ROPE, back.preset("b2"));
        assertNull(back.preset("gone"));
    }

    @Test
    void chapterWithOnlyAPresetIsKept() {
        ChapterExtras c = new ChapterExtras();
        c.linePreset = "a1";
        assertFalse(c.isEmpty());
        assertEquals("a1", c.copy().linePreset);
    }

    @Test
    void patchCarriesPresetsAndUndoes() {
        BookExtras b = new BookExtras();
        b.linePresets.add(CHAIN);
        EditorPatch p = new EditorPatch();
        p.presets = List.of(CHAIN, ROPE);
        EditorPatch wire = EditorPatch.read(p.write());
        assertNotNull(wire);
        EditorPatch undo = wire.snapshot(b);
        wire.applyTo(b);
        assertEquals(List.of(CHAIN, ROPE), b.linePresets);
        EditorPatch.read(undo.write()).applyTo(b);
        assertEquals(List.of(CHAIN), b.linePresets);
    }

    @Test
    void invalidPresetsAreDropped() {
        EditorPatch p = new EditorPatch();
        p.presets = List.of(new LinePreset("BAD ID", "x", "a:b.png"), new LinePreset("ok", "x".repeat(65), "a:b.png"),
                CHAIN, new LinePreset("a1", "Duplicate id", "a:b.png"));
        EditorPatch wire = EditorPatch.read(p.write());
        assertNotNull(wire);
        assertEquals(List.of(CHAIN), wire.presets);
    }
}
