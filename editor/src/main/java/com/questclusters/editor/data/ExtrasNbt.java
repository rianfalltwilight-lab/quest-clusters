package com.questclusters.editor.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Sidecar file format ({@code quests/quest_clusters/editor/}): {@code book.snbt} and one {@code <chapter id>.snbt}
 * per chapter. A chapter file also carries the quest and image data of the objects currently in that chapter.
 * The same layout (all chapters in one compound) is used for the client sync.
 */
public final class ExtrasNbt {
    public static final int VERSION = 1;

    private ExtrasNbt() {
    }

    // ------------------------------------------------------------------ book

    public static CompoundTag writeBook(BookExtras b, Supplier<CompoundTag> factory) {
        CompoundTag t = factory.get();
        t.putInt("version", VERSION);
        if (!b.sidebarBackground.isEmpty()) t.putString("sidebar_background", b.sidebarBackground);
        return t;
    }

    public static void readBook(CompoundTag t, BookExtras into) {
        into.sidebarBackground = t.getString("sidebar_background");
    }

    // ------------------------------------------------------------------ chapter

    /** Null if there is nothing to write (the file should then be deleted). */
    public static CompoundTag writeChapter(long chapterId, ChapterExtras c, Map<Long, QuestExtras> quests,
                                           Collection<Long> excludedImages, Supplier<CompoundTag> factory) {
        boolean empty = (c == null || c.isEmpty()) && quests.isEmpty() && excludedImages.isEmpty();
        if (empty) return null;
        CompoundTag t = factory.get();
        t.putInt("version", VERSION);
        t.putString("chapter", NodeKeys.hex(chapterId));
        if (c != null) {
            if (!c.background.isEmpty()) t.putString("background", c.background);
            if (!c.prerequisites.isEmpty()) {
                ListTag l = new ListTag();
                c.prerequisites.forEach(id -> l.add(StringTag.valueOf(NodeKeys.hex(id))));
                t.put("prerequisites", l);
            }
            if (!c.decorativeLines.isEmpty()) {
                ListTag l = new ListTag();
                for (List<String> line : c.decorativeLines) {
                    CompoundTag lt = factory.get();
                    ListTag nodes = new ListTag();
                    line.forEach(n -> nodes.add(StringTag.valueOf(n)));
                    lt.put("nodes", nodes);
                    l.add(lt);
                }
                t.put("decorative_lines", l);
            }
            if (!c.hiddenLines.isEmpty()) {
                ListTag l = new ListTag();
                for (ChapterExtras.HiddenLine h : c.hiddenLines) {
                    CompoundTag ht = factory.get();
                    ht.putString("source", h.source());
                    ht.putString("dependency", h.dependency());
                    if (h.revealOnHover()) ht.putBoolean("reveal_on_hover", true);
                    l.add(ht);
                }
                t.put("hidden_lines", l);
            }
        }
        if (!quests.isEmpty()) {
            ListTag l = new ListTag();
            quests.forEach((id, q) -> {
                CompoundTag qt = factory.get();
                qt.putString("id", NodeKeys.hex(id));
                if (!q.boxBackground().isEmpty()) qt.putString("box_background", q.boxBackground());
                if (!q.viewBackground().isEmpty()) qt.putString("view_background", q.viewBackground());
                if (q.disableScrollPaging()) qt.putBoolean("disable_scroll_paging", true);
                l.add(qt);
            });
            t.put("quests", l);
        }
        if (!excludedImages.isEmpty()) {
            ListTag l = new ListTag();
            excludedImages.stream().sorted().forEach(id -> l.add(StringTag.valueOf(NodeKeys.hex(id))));
            t.put("box_select_excluded_images", l);
        }
        return t;
    }

    /** Reads a chapter file into the book. Returns the chapter id, or null if the file is malformed. */
    public static Long readChapter(CompoundTag t, BookExtras into) {
        Long chapterId = NodeKeys.parseHex(t.getString("chapter"));
        if (chapterId == null) return null;
        ChapterExtras c = new ChapterExtras();
        c.background = t.getString("background");
        for (Tag e : t.getList("prerequisites", Tag.TAG_STRING)) {
            Long id = NodeKeys.parseHex(e.getAsString());
            if (id != null && !c.prerequisites.contains(id)) c.prerequisites.add(id);
        }
        ListTag lines = t.getList("decorative_lines", Tag.TAG_COMPOUND);
        for (int i = 0; i < lines.size(); i++) {
            List<String> nodes = new ArrayList<>();
            for (Tag n : lines.getCompound(i).getList("nodes", Tag.TAG_STRING)) nodes.add(n.getAsString());
            List<String> norm = ChapterExtras.normaliseLine(nodes);
            if (norm != null) c.decorativeLines.add(norm);
        }
        ListTag hidden = t.getList("hidden_lines", Tag.TAG_COMPOUND);
        for (int i = 0; i < hidden.size(); i++) {
            CompoundTag h = hidden.getCompound(i);
            String s = h.getString("source"), d = h.getString("dependency");
            if (!s.isBlank() && !d.isBlank()) c.hiddenLines.add(new ChapterExtras.HiddenLine(s, d, h.getBoolean("reveal_on_hover")));
        }
        if (!c.isEmpty()) into.chapters.put(chapterId, c);
        ListTag quests = t.getList("quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < quests.size(); i++) {
            CompoundTag q = quests.getCompound(i);
            Long id = NodeKeys.parseHex(q.getString("id"));
            if (id != null) {
                into.setQuest(id, new QuestExtras(q.getString("box_background"), q.getString("view_background"),
                        q.getBoolean("disable_scroll_paging")));
            }
        }
        for (Tag e : t.getList("box_select_excluded_images", Tag.TAG_STRING)) {
            Long id = NodeKeys.parseHex(e.getAsString());
            if (id != null) into.excludedImages.add(id);
        }
        return chapterId;
    }

    // ------------------------------------------------------------------ whole book (client sync)

    public static CompoundTag writeAll(BookExtras b) {
        CompoundTag t = writeBook(b, CompoundTag::new);
        ListTag chapters = new ListTag();
        b.chapters.forEach((id, c) -> {
            CompoundTag ct = writeChapter(id, c, Map.of(), List.of(), CompoundTag::new);
            if (ct != null) chapters.add(ct);
        });
        t.put("chapters", chapters);
        // quest and image data are chapter-independent: one pseudo chapter (id 0) carries them
        CompoundTag rest = writeChapter(0L, null, b.quests, b.excludedImages, CompoundTag::new);
        if (rest != null) t.put("objects", rest);
        return t;
    }

    public static BookExtras readAll(CompoundTag t) {
        BookExtras b = new BookExtras();
        readBook(t, b);
        ListTag chapters = t.getList("chapters", Tag.TAG_COMPOUND);
        for (int i = 0; i < chapters.size(); i++) readChapter(chapters.getCompound(i), b);
        if (t.contains("objects")) readChapter(t.getCompound("objects"), b);
        return b;
    }
}
