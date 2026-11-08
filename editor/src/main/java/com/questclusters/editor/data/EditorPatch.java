package com.questclusters.editor.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One editor change: full replacement values for the parts it touches (sidebar, some chapters, some quests,
 * some images). {@link #snapshot} of the current data gives the patch that undoes it.
 */
public final class EditorPatch {
    /** Null = sidebar untouched. */
    public String sidebar;
    public final Map<Long, ChapterExtras> chapters = new LinkedHashMap<>();
    public final Map<Long, QuestExtras> quests = new LinkedHashMap<>();
    /** Image id → excluded from box selection. */
    public final Map<Long, Boolean> images = new LinkedHashMap<>();
    /** Null = presets untouched; otherwise the complete new list of decorative line presets. */
    public List<LinePreset> presets;

    public boolean isEmpty() {
        return sidebar == null && chapters.isEmpty() && quests.isEmpty() && images.isEmpty() && presets == null;
    }

    /** The current values of everything this patch touches. */
    public EditorPatch snapshot(BookExtras b) {
        EditorPatch p = new EditorPatch();
        if (sidebar != null) p.sidebar = b.sidebarBackground;
        chapters.keySet().forEach(id -> p.chapters.put(id, b.chapter(id).copy()));
        quests.keySet().forEach(id -> p.quests.put(id, b.quest(id)));
        images.keySet().forEach(id -> p.images.put(id, b.excludedImages.contains(id)));
        if (presets != null) p.presets = List.copyOf(b.linePresets);
        return p;
    }

    public void applyTo(BookExtras b) {
        if (sidebar != null) b.sidebarBackground = sidebar;
        chapters.forEach((id, c) -> {
            if (c.isEmpty()) b.chapters.remove(id);
            else b.chapters.put(id, c.copy());
        });
        quests.forEach(b::setQuest);
        images.forEach((id, ex) -> {
            if (ex) b.excludedImages.add(id);
            else b.excludedImages.remove(id);
        });
        if (presets != null) {
            b.linePresets.clear();
            b.linePresets.addAll(presets);
        }
    }

    // ------------------------------------------------------------------ wire format

    public CompoundTag write() {
        CompoundTag t = new CompoundTag();
        if (sidebar != null) t.putString("sidebar", sidebar);
        ListTag ch = new ListTag();
        chapters.forEach((id, c) -> {
            CompoundTag ct = ExtrasNbt.writeChapter(id, c, Map.of(), List.of(), CompoundTag::new);
            if (ct == null) {
                ct = new CompoundTag();
                ct.putString("chapter", NodeKeys.hex(id));
            }
            ch.add(ct);
        });
        t.put("chapters", ch);
        ListTag q = new ListTag();
        quests.forEach((id, e) -> {
            CompoundTag qt = new CompoundTag();
            qt.putString("id", NodeKeys.hex(id));
            qt.putString("box_background", e.boxBackground());
            qt.putString("view_background", e.viewBackground());
            qt.putBoolean("disable_scroll_paging", e.disableScrollPaging());
            q.add(qt);
        });
        t.put("quests", q);
        ListTag im = new ListTag();
        images.forEach((id, ex) -> {
            CompoundTag it = new CompoundTag();
            it.putString("id", NodeKeys.hex(id));
            it.putBoolean("excluded", ex);
            im.add(it);
        });
        t.put("images", im);
        if (presets != null) t.put("presets", ExtrasNbt.writePresets(presets, CompoundTag::new));
        return t;
    }

    /** Parses and bounds-checks; returns null if malformed or oversized. */
    public static EditorPatch read(CompoundTag t) {
        EditorPatch p = new EditorPatch();
        if (t.contains("sidebar", Tag.TAG_STRING)) {
            p.sidebar = cleanLocation(t.getString("sidebar"));
            if (p.sidebar == null) return null;
        }
        ListTag ch = t.getList("chapters", Tag.TAG_COMPOUND);
        if (ch.size() > 512) return null;
        for (int i = 0; i < ch.size(); i++) {
            CompoundTag ct = ch.getCompound(i);
            BookExtras tmp = new BookExtras();
            Long id = ExtrasNbt.readChapter(ct, tmp);
            if (id == null) return null;
            ChapterExtras c = tmp.chapter(id).copy();
            if (cleanLocation(c.background) == null || c.decorativeLines.size() > 4096 || c.hiddenLines.size() > 4096
                    || c.prerequisites.size() > 1024) return null;
            for (List<String> line : c.decorativeLines) {
                if (line.size() > 256 || line.stream().anyMatch(n -> n.length() > 128)) return null;
            }
            for (ChapterExtras.HiddenLine h : c.hiddenLines) {
                if (h.source().length() > 128 || h.dependency().length() > 128) return null;
            }
            if (c.linePreset.length() > 32 || c.nodeLinePresets.size() > 4096) return null;
            for (var e : c.nodeLinePresets.entrySet()) {
                if (e.getKey().length() > 128 || e.getValue().length() > 32) return null;
            }
            p.chapters.put(id, c);
        }
        ListTag q = t.getList("quests", Tag.TAG_COMPOUND);
        if (q.size() > 8192) return null;
        for (int i = 0; i < q.size(); i++) {
            CompoundTag qt = q.getCompound(i);
            Long id = NodeKeys.parseHex(qt.getString("id"));
            String box = cleanLocation(qt.getString("box_background")), view = cleanLocation(qt.getString("view_background"));
            if (id == null || box == null || view == null) return null;
            p.quests.put(id, new QuestExtras(box, view, qt.getBoolean("disable_scroll_paging")));
        }
        ListTag im = t.getList("images", Tag.TAG_COMPOUND);
        if (im.size() > 8192) return null;
        for (int i = 0; i < im.size(); i++) {
            Long id = NodeKeys.parseHex(im.getCompound(i).getString("id"));
            if (id == null) return null;
            p.images.put(id, im.getCompound(i).getBoolean("excluded"));
        }
        if (t.contains("presets", Tag.TAG_LIST)) {
            ListTag pl = t.getList("presets", Tag.TAG_COMPOUND);
            if (pl.size() > LinePreset.MAX) return null;
            p.presets = ExtrasNbt.readPresets(pl);
        }
        return p;
    }

    /** "" stays "", a valid location is kept, anything else is rejected (null). */
    static String cleanLocation(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= 256 && CanvasElement.validLocation(s) ? s : null;
    }
}
