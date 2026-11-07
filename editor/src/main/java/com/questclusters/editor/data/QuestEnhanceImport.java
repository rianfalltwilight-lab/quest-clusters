package com.questclusters.editor.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads the keys Quest Enhance (3.x) wrote into FTB Quests' own files. Pure: raw NBT in, {@link BookExtras} out.
 * Accepts both boolean spellings ({@code true}/{@code 1b}) and the legacy forms Quest Enhance itself migrated
 * (quest ids as longs instead of node keys).
 */
public final class QuestEnhanceImport {
    public static final String KEY_PREFIX = "quest_enhance_";
    public static final String SIDEBAR = "quest_enhance_chapter_sidebar_background";
    public static final String CHAPTER_BG = "quest_enhance_chapter_background";
    public static final String PREREQS = "quest_enhance_chapter_quest_prerequisites";
    public static final String DECO = "quest_enhance_decorative_lines";
    public static final String HIDDEN = "quest_enhance_hidden_dependency_lines";
    public static final String QUEST_BG = "quest_enhance_quest_background";
    public static final String QUEST_VIEW_BG = "quest_enhance_quest_view_background";
    public static final String SCROLL = "quest_enhance_disable_scroll_paging";
    public static final String EXCLUDE = "quest_enhance_exclude_from_box_selection";

    /** Summary counts, for the migration report and tests. */
    public record Stats(int chapters, int chapterBackgrounds, int prerequisites, int decorativeLines, int hiddenLines,
                        int questBackgrounds, int viewBackgrounds, int scrollPaging, int excludedImages,
                        int legacyClickFixes, boolean sidebar) {
        @Override
        public String toString() {
            return chapters + " chapters: " + chapterBackgrounds + " chapter backgrounds, " + prerequisites
                    + " chapter prerequisites, " + decorativeLines + " decorative lines, " + hiddenLines + " hidden lines, "
                    + questBackgrounds + " quest box backgrounds, " + viewBackgrounds + " details backgrounds, "
                    + scrollPaging + " no-scroll-paging quests, " + excludedImages + " box-select exclusions, "
                    + legacyClickFixes + " legacy canvas elements" + (sidebar ? ", sidebar background" : "");
        }
    }

    /** A chapter image whose Quest Enhance payload sits in the legacy {@code click} key FTB Quests 2101 no longer reads. */
    public record LegacyClick(String chapterFile, long imageId, String data) {
    }

    public record Result(BookExtras book, List<LegacyClick> legacyClicks, Stats stats) {
    }

    private QuestEnhanceImport() {
    }

    public static boolean hasData(CompoundTag fileData, Map<String, CompoundTag> chapters) {
        if (fileData.contains(SIDEBAR)) return true;
        for (CompoundTag ch : chapters.values()) {
            for (String k : ch.getAllKeys()) {
                if (k.startsWith(KEY_PREFIX)) return true;
            }
            if (anyLegacyClick(ch)) return true;
        }
        return false;
    }

    private static boolean anyLegacyClick(CompoundTag ch) {
        ListTag images = ch.getList("images", Tag.TAG_COMPOUND);
        for (int i = 0; i < images.size(); i++) {
            if (images.getCompound(i).getString("click").startsWith(CanvasElement.PREFIX)) return true;
        }
        return false;
    }

    public static Result read(CompoundTag fileData, Map<String, CompoundTag> chapters) {
        BookExtras b = new BookExtras();
        List<LegacyClick> clicks = new ArrayList<>();
        int nCh = 0, nBg = 0, nPre = 0, nDeco = 0, nHidden = 0, nQBg = 0, nVBg = 0, nScroll = 0, nEx = 0;
        b.sidebarBackground = location(fileData.getString(SIDEBAR));

        for (Map.Entry<String, CompoundTag> chEntry : chapters.entrySet()) {
            CompoundTag ch = chEntry.getValue();
            Long chapterId = id(ch.get("id"));
            if (chapterId == null) continue;
            nCh++;
            ChapterExtras c = new ChapterExtras();
            c.background = location(ch.getString(CHAPTER_BG));
            if (!c.background.isEmpty()) nBg++;

            Tag pre = ch.get(PREREQS);
            if (pre instanceof ListTag pl) {
                for (Tag e : pl) addPrereq(c, chapterId, id(e));
            } else if (pre instanceof LongArrayTag la) {
                for (long v : la.getAsLongArray()) addPrereq(c, chapterId, v);
            }
            nPre += c.prerequisites.size();

            ListTag deco = ch.getList(DECO, Tag.TAG_COMPOUND);
            for (int i = 0; i < deco.size(); i++) {
                Tag nodesTag = deco.getCompound(i).get("nodes");
                List<String> nodes = new ArrayList<>();
                if (nodesTag instanceof LongArrayTag la) {
                    for (long v : la.getAsLongArray()) nodes.add(NodeKeys.quest(v));
                } else if (nodesTag instanceof ListTag nl) {
                    for (Tag n : nl) nodes.add(n instanceof NumericTag num ? NodeKeys.quest(num.getAsLong()) : n.getAsString());
                }
                List<String> norm = ChapterExtras.normaliseLine(nodes);
                if (norm != null) {
                    c.decorativeLines.add(norm);
                    nDeco++;
                }
            }

            ListTag hidden = ch.getList(HIDDEN, Tag.TAG_COMPOUND);
            for (int i = 0; i < hidden.size(); i++) {
                CompoundTag h = hidden.getCompound(i);
                String s = nodeOrQuest(h.get("source")), d = nodeOrQuest(h.get("dependency"));
                if (s != null && d != null && c.hiddenLine(s, d).isEmpty()) {
                    c.hiddenLines.add(new ChapterExtras.HiddenLine(s, d, h.getBoolean("reveal_on_hover")));
                    nHidden++;
                }
            }
            if (!c.isEmpty()) b.chapters.put(chapterId, c);

            ListTag quests = ch.getList("quests", Tag.TAG_COMPOUND);
            for (int i = 0; i < quests.size(); i++) {
                CompoundTag q = quests.getCompound(i);
                Long qid = id(q.get("id"));
                if (qid == null) continue;
                QuestExtras e = new QuestExtras(location(q.getString(QUEST_BG)), location(q.getString(QUEST_VIEW_BG)), q.getBoolean(SCROLL));
                if (!e.boxBackground().isEmpty()) nQBg++;
                if (!e.viewBackground().isEmpty()) nVBg++;
                if (e.disableScrollPaging()) nScroll++;
                b.setQuest(qid, e);
            }

            ListTag images = ch.getList("images", Tag.TAG_COMPOUND);
            for (int i = 0; i < images.size(); i++) {
                CompoundTag img = images.getCompound(i);
                Long iid = id(img.get("id"));
                if (iid == null) continue;
                if (img.getBoolean(EXCLUDE)) {
                    b.excludedImages.add(iid);
                    nEx++;
                }
                String click = img.getString("click");
                if (click.startsWith(CanvasElement.PREFIX) && !img.contains("click_action")) {
                    clicks.add(new LegacyClick(chEntry.getKey(), iid, click));
                }
            }
        }
        return new Result(b, clicks, new Stats(nCh, nBg, nPre, nDeco, nHidden, nQBg, nVBg, nScroll, nEx, clicks.size(),
                !b.sidebarBackground.isEmpty()));
    }

    /**
     * Rewrites legacy {@code click: "quest_enhance:..."} entries of one raw chapter as
     * {@code click_action: "none:quest_enhance:..."} (what FTB Quests 2101 reads). Returns how many were changed.
     */
    public static int fixLegacyClicks(CompoundTag chapter) {
        int n = 0;
        ListTag images = chapter.getList("images", Tag.TAG_COMPOUND);
        for (int i = 0; i < images.size(); i++) {
            CompoundTag img = images.getCompound(i);
            String click = img.getString("click");
            if (click.startsWith(CanvasElement.PREFIX) && !img.contains("click_action")) {
                img.remove("click");
                img.putString("click_action", "none:" + click);
                n++;
            }
        }
        return n;
    }

    /**
     * Adds what the import found to existing data without overwriting anything already set there
     * (a book that was partly edited with the editor before the import keeps those edits).
     */
    public static void mergeInto(BookExtras from, BookExtras into) {
        if (into.sidebarBackground.isEmpty()) into.sidebarBackground = from.sidebarBackground;
        from.chapters.forEach((id, src) -> {
            ChapterExtras dst = into.editChapter(id);
            if (dst.background.isEmpty()) dst.background = src.background;
            src.prerequisites.forEach(p -> {
                if (!dst.prerequisites.contains(p)) dst.prerequisites.add(p);
            });
            for (List<String> line : src.decorativeLines) {
                if (dst.decorativeLines.stream().noneMatch(l -> l.containsAll(line) && line.containsAll(l))) dst.decorativeLines.add(line);
            }
            for (ChapterExtras.HiddenLine h : src.hiddenLines) {
                if (dst.hiddenLine(h.source(), h.dependency()).isEmpty()) dst.hiddenLines.add(h);
            }
        });
        from.quests.forEach((id, q) -> {
            QuestExtras cur = into.quest(id);
            into.setQuest(id, new QuestExtras(cur.boxBackground().isEmpty() ? q.boxBackground() : cur.boxBackground(),
                    cur.viewBackground().isEmpty() ? q.viewBackground() : cur.viewBackground(),
                    cur.disableScrollPaging() || q.disableScrollPaging()));
        });
        into.excludedImages.addAll(from.excludedImages);
    }

    private static void addPrereq(ChapterExtras c, long chapterId, Long id) {
        if (id != null && id != chapterId && id != 0L && !c.prerequisites.contains(id)) c.prerequisites.add(id);
    }

    /** FTB Quests ids are hex strings in files; older data may hold plain longs. */
    static Long id(Tag t) {
        if (t instanceof NumericTag n) return n.getAsLong();
        if (t != null && t.getId() == Tag.TAG_STRING) return NodeKeys.parseHex(t.getAsString());
        return null;
    }

    private static String nodeOrQuest(Tag t) {
        if (t instanceof NumericTag n) return NodeKeys.quest(n.getAsLong());
        if (t == null || t.getId() != Tag.TAG_STRING) return null;
        String s = t.getAsString().trim();
        return s.isEmpty() ? null : s;
    }

    /** Valid resource location or "" (Quest Enhance treats unparsable values as unset). */
    static String location(String s) {
        if (s == null) return "";
        s = s.trim();
        return CanvasElement.validLocation(s) ? s : "";
    }
}
