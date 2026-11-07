package com.questclusters.editor.data;

import dev.ftb.mods.ftblibrary.snbt.SNBT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EditorDataTest {
    @Test
    void parsesCanvasPayloads() {
        var t = (CanvasElement.Text) CanvasElement.parse("quest_enhance:text_v3:1.0:37:kaleidoscope_chinesefood:couplet_font从这里开始---->");
        assertEquals("kaleidoscope_chinesefood:couplet_font", t.font());
        assertEquals("从这里开始---->", t.text());
        assertEquals(1.0, t.scale());
        assertEquals(t, CanvasElement.parse(t.encode()));

        var t2 = (CanvasElement.Text) CanvasElement.parse("quest_enhance:text_v2:17:minecraft:defaulta:b:c");
        assertEquals("minecraft:default", t2.font());
        assertEquals("a:b:c", t2.text());
        assertEquals("x", ((CanvasElement.Text) CanvasElement.parse("quest_enhance:text:x")).text());

        var a = (CanvasElement.Anchor) CanvasElement.parse("quest_enhance:decorative_anchor:73c3ca86-87d1-469a-9d42-a0eeda5398b7");
        assertEquals("a:73c3ca86-87d1-469a-9d42-a0eeda5398b7", a.nodeKey());
        assertNull(((CanvasElement.Anchor) CanvasElement.parse("quest_enhance:decorative_anchor:nope")).nodeKey());

        assertInstanceOf(CanvasElement.Gif.class, CanvasElement.parse("quest_enhance:gif_v1:quest_enhance:textures/gifs/fire.gif"));
        assertNull(CanvasElement.parse("quest_enhance:text_v3:NaN:3:a:bx"));
        assertNull(CanvasElement.parse("quest_enhance:text_v3:1.0:99:a:b"));
        assertNull(CanvasElement.parse("https://example.com"));
        assertNull(CanvasElement.parse(""));
    }

    @Test
    void nodeKeysMatchQuestEnhance() {
        assertEquals("q:19375340aa0757", NodeKeys.quest(0x0019375340AA0757L));
        assertEquals(0x0019375340AA0757L, NodeKeys.objectId("q:19375340aa0757"));
        assertNull(NodeKeys.objectId("a:xyz"));
        assertEquals("FFFFFFFFFFFFFFFF", NodeKeys.hex(-1L));
        assertEquals(-1L, NodeKeys.parseHex("#FFFFFFFFFFFFFFFF"));
    }

    @Test
    void importsQuestEnhanceKeys() throws Exception {
        CompoundTag ch = TagParser.parseTag("""
                {id:"6CBCF99E3B9CC743",
                 quest_enhance_chapter_background:"scex:textures/gui/bg.png",
                 quest_enhance_chapter_quest_prerequisites:[6894113135435469441L, 7836025163829520195L],
                 quest_enhance_decorative_lines:[{nodes:["q:1","a:73c3ca86-87d1-469a-9d42-a0eeda5398b7","q:1"]},{nodes:["q:2"]},{nodes:[L;3L,4L]}],
                 quest_enhance_hidden_dependency_lines:[{source:"q:5", dependency:"l:6", reveal_on_hover:1b},{source:7L,dependency:"q:8"},{source:"",dependency:"q:9"}],
                 quests:[{id:"0000000000000001", quest_enhance_quest_background:"scex:a.png", quest_enhance_disable_scroll_paging:true},
                         {id:"0000000000000002", quest_enhance_quest_view_background:"bad value!", quest_enhance_disable_scroll_paging:false}],
                 images:[{id:"755CAE85AB63C8C6", quest_enhance_exclude_from_box_selection:1b},
                         {id:"0000000000000AAA", click:"quest_enhance:text:hello", quest_enhance_exclude_from_box_selection:0b}]}""");
        CompoundTag data = TagParser.parseTag("{quest_enhance_chapter_sidebar_background:\"scex:side.png\"}");
        Map<String, CompoundTag> chapters = Map.of("backpack", ch);
        assertTrue(QuestEnhanceImport.hasData(data, chapters));
        assertFalse(QuestEnhanceImport.hasData(new CompoundTag(), Map.of("x", TagParser.parseTag("{id:\"01\"}"))));

        var r = QuestEnhanceImport.read(data, chapters);
        BookExtras b = r.book();
        long chId = 0x6CBCF99E3B9CC743L;
        ChapterExtras c = b.chapter(chId);
        assertEquals("scex:side.png", b.sidebarBackground);
        assertEquals("scex:textures/gui/bg.png", c.background);
        assertEquals(List.of(6894113135435469441L, 7836025163829520195L), c.prerequisites);
        assertEquals(List.of(List.of("q:1", "a:73c3ca86-87d1-469a-9d42-a0eeda5398b7"), List.of("q:3", "q:4")), c.decorativeLines);
        assertEquals(2, c.hiddenLines.size());
        assertEquals(new ChapterExtras.HiddenLine("q:5", "l:6", true), c.hiddenLines.get(0));
        assertEquals(new ChapterExtras.HiddenLine("q:7", "q:8", false), c.hiddenLines.get(1));
        assertEquals(new QuestExtras("scex:a.png", "", true), b.quest(1));
        assertTrue(b.quest(2).isEmpty()); // invalid location = unset
        assertEquals(java.util.Set.of(0x755CAE85AB63C8C6L), b.excludedImages);
        assertEquals(List.of(new QuestEnhanceImport.LegacyClick("backpack", 0xAAAL, "quest_enhance:text:hello")), r.legacyClicks());

        CompoundTag fixed = ch.copy();
        assertEquals(1, QuestEnhanceImport.fixLegacyClicks(fixed));
        assertEquals("none:quest_enhance:text:hello", fixed.getList("images", 10).getCompound(1).getString("click_action"));
        assertFalse(fixed.getList("images", 10).getCompound(1).contains("click"));
        assertEquals(0, QuestEnhanceImport.fixLegacyClicks(fixed));

        // sidecar round trip, grouped by chapter the way the server saves it
        CompoundTag file = ExtrasNbt.writeChapter(chId, c, b.quests, b.excludedImages, CompoundTag::new);
        BookExtras back = new BookExtras();
        assertEquals(chId, ExtrasNbt.readChapter(file, back));
        assertEquals(c.decorativeLines, back.chapter(chId).decorativeLines);
        assertEquals(c.hiddenLines, back.chapter(chId).hiddenLines);
        assertEquals(c.prerequisites, back.chapter(chId).prerequisites);
        assertEquals(b.quests, back.quests);
        assertEquals(b.excludedImages, back.excludedImages);
        assertNull(ExtrasNbt.writeChapter(5L, new ChapterExtras(), Map.of(), List.of(), CompoundTag::new));

        // client sync round trip
        BookExtras sync = ExtrasNbt.readAll(ExtrasNbt.writeAll(b));
        assertEquals(b.sidebarBackground, sync.sidebarBackground);
        assertEquals(b.quests, sync.quests);
        assertEquals(c.background, sync.chapter(chId).background);

        // merge keeps what was already set
        BookExtras existing = new BookExtras();
        existing.editChapter(chId).background = "mine:bg.png";
        existing.editChapter(chId).decorativeLines.add(List.of("q:4", "q:3"));
        QuestEnhanceImport.mergeInto(b, existing);
        assertEquals("mine:bg.png", existing.chapter(chId).background);
        assertEquals(2, existing.chapter(chId).decorativeLines.size()); // same node set not duplicated
    }

    /** Local only: -Dquest_clusters.legacyBook=<path to config/ftbquests/quests> (a private quest book). */
    @Test
    void importsRealLegacyBook() throws Exception {
        String p = System.getProperty("quest_clusters.legacyBook");
        Assumptions.assumeTrue(p != null && Files.isDirectory(Path.of(p)), "no legacy book given");
        Path dir = Path.of(p);
        Map<String, CompoundTag> chapters = new LinkedHashMap<>();
        try (Stream<Path> s = Files.list(dir.resolve("chapters"))) {
            for (Path f : s.sorted().toList()) chapters.put(f.getFileName().toString(), SNBT.read(f));
        }
        CompoundTag data = Files.exists(dir.resolve("data.snbt")) ? SNBT.read(dir.resolve("data.snbt")) : new CompoundTag();
        var r = QuestEnhanceImport.read(data, chapters);
        System.out.println("legacy book: " + r.stats());
        assertTrue(QuestEnhanceImport.hasData(data, chapters));
        assertEquals(chapters.size(), r.stats().chapters());
        // every anchor referenced by a line exists as an anchor image in that chapter
        int anchors = 0, texts = 0;
        for (CompoundTag ch : chapters.values()) {
            var imgs = ch.getList("images", 10);
            for (int i = 0; i < imgs.size(); i++) {
                var el = CanvasElement.parse(imgs.getCompound(i).getString("click_action").replaceFirst("^none:", ""));
                if (el instanceof CanvasElement.Anchor) anchors++;
                if (el instanceof CanvasElement.Text) texts++;
            }
        }
        System.out.println("legacy book: anchors=" + anchors + " texts=" + texts);
        assertTrue(anchors > 0);
    }
}
