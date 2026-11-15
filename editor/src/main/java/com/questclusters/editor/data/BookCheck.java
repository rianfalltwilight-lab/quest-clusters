package com.questclusters.editor.data;

import com.questclusters.api.QuestClustersAPI;
import com.questclusters.data.QuestCluster;
import dev.ftb.mods.ftbquests.item.MissingItem;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.RandomReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.translation.TranslationKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Book check: finds problems in a quest book that FTB Quests itself does not point out. Runs on either side; texture
 * checks need the client's resources and run only when a {@code textureExists} test is given.
 */
public final class BookCheck {
    public enum Level { ERROR, WARN, INFO }

    /**
     * One problem.
     *
     * @param object id of the quest object to jump to (0 = none), {@code chapter} the chapter it is in (0 = none)
     */
    public record Issue(Level level, String kind, long object, long chapter, Component text) {
    }

    private BookCheck() {
    }

    /**
     * @param textureExists null on the server (texture checks skipped)
     */
    public static List<Issue> run(BaseQuestFile file, BookExtras book, Predicate<String> textureExists) {
        List<Issue> out = new ArrayList<>();
        loops(file, out);
        file.forAllChapters(ch -> chapter(file, ch, out));
        file.forAllQuests(q -> quest(file, q, out));
        rewardTables(file, out);
        clusters(file, out);
        if (book != null) editorData(file, book, textureExists, out);
        if (textureExists != null) file.forAllChapters(ch -> {
            for (ChapterImage img : ch.getImages()) {
                String tex = imageTexture(img);
                if (tex != null && !textureExists.test(tex)) {
                    out.add(issue(Level.ERROR, "missing_image", img.id, ch.id, tex));
                }
            }
        });
        out.sort(Comparator.comparing(Issue::level));
        return out;
    }

    private static Issue issue(Level level, String kind, long object, long chapter, Object... args) {
        return new Issue(level, kind, object, chapter, Component.translatable("quest_clusters_editor.check." + kind, args));
    }

    private static long chapterOf(QuestObjectBase o) {
        return o != null && o.getQuestChapter() != null ? o.getQuestChapter().id : 0L;
    }

    // ------------------------------------------------------------------ quests

    /** Quests whose prerequisites lead back to themselves: none of them can ever start. */
    private static void loops(BaseQuestFile file, List<Issue> out) {
        Map<Quest, Integer> state = new HashMap<>(); // 1 = on the path, 2 = done
        Set<Quest> reported = new HashSet<>();
        file.forAllQuests(q -> visit(q, state, new ArrayList<>(), reported, out));
    }

    private static void visit(Quest q, Map<Quest, Integer> state, List<Quest> path, Set<Quest> reported, List<Issue> out) {
        Integer s = state.get(q);
        if (s != null) {
            if (s == 1) {
                List<Quest> loop = path.subList(path.indexOf(q), path.size());
                if (loop.stream().noneMatch(reported::contains)) {
                    reported.addAll(loop);
                    StringBuilder names = new StringBuilder();
                    for (Quest l : loop) names.append(l.getTitle().getString()).append(" → ");
                    names.append(q.getTitle().getString());
                    out.add(issue(Level.ERROR, "loop", q.id, chapterOf(q), names.toString()));
                }
            }
            return;
        }
        state.put(q, 1);
        path.add(q);
        for (QuestObject d : q.streamDependencies().toList()) {
            if (d instanceof Quest p) visit(p, state, path, reported, out);
        }
        path.remove(path.size() - 1);
        state.put(q, 2);
    }

    private static void quest(BaseQuestFile file, Quest q, List<Issue> out) {
        long ch = chapterOf(q);
        if (q.getTasks().isEmpty()) out.add(issue(Level.INFO, "no_tasks", q.id, ch, q.getTitle()));
        boolean visibleChapter = q.getChapter() == null || !q.getChapter().isAlwaysInvisible();
        for (QuestObject d : q.streamDependencies().toList()) {
            if (visibleChapter && d instanceof Quest p && p.getChapter() != null && p.getChapter().isAlwaysInvisible()) {
                out.add(issue(Level.WARN, "hidden_prerequisite", q.id, ch, q.getTitle(), p.getTitle()));
            }
        }
        for (Task t : q.getTasks()) {
            if (t instanceof ItemTask it && missing(it.getItemStack())) out.add(issue(Level.ERROR, "missing_item_task", q.id, ch, q.getTitle()));
        }
        for (Reward r : q.getRewards()) {
            if (r instanceof ItemReward ir && missing(ir.getItem())) out.add(issue(Level.ERROR, "missing_item_reward", q.id, ch, q.getTitle()));
        }
        TranslationKey title = TranslationKey.TITLE;
        if (file.getTranslationManager().hasMissingTranslation(q, title)) {
            out.add(issue(Level.INFO, "missing_translation", q.id, ch, q.getTitle()));
        }
    }

    private static boolean missing(ItemStack stack) {
        return stack != null && stack.getItem() instanceof MissingItem;
    }

    private static void chapter(BaseQuestFile file, Chapter ch, List<Issue> out) {
        if (ch.getQuests().isEmpty() && ch.getQuestLinks().isEmpty() && ch.getImages().isEmpty()) {
            out.add(issue(Level.WARN, "empty_chapter", ch.id, ch.id, ch.getTitle()));
        }
        if (file.getTranslationManager().hasMissingTranslation(ch, TranslationKey.TITLE)) {
            out.add(issue(Level.INFO, "missing_translation", ch.id, ch.id, ch.getTitle()));
        }
    }

    // ------------------------------------------------------------------ reward tables

    /** Tables no reward points at and that have no loot crate (nothing can ever give them out). */
    private static void rewardTables(BaseQuestFile file, List<Issue> out) {
        Set<RewardTable> used = new HashSet<>();
        file.forAllQuests(q -> {
            for (Reward r : q.getRewards()) {
                if (r instanceof RandomReward rr && rr.getTable() != null) used.add(rr.getTable());
            }
        });
        for (RewardTable t : file.getRewardTables()) {
            if (!used.contains(t) && t.getLootCrate() == null) out.add(issue(Level.INFO, "unused_table", t.id, 0L, t.getTitle()));
            for (var w : t.getWeightedRewards()) {
                if (w.getReward() instanceof ItemReward ir && missing(ir.getItem())) {
                    out.add(issue(Level.ERROR, "missing_item_table", t.id, 0L, t.getTitle()));
                }
            }
        }
    }

    // ------------------------------------------------------------------ clusters and editor data

    private static void clusters(BaseQuestFile file, List<Issue> out) {
        for (QuestCluster c : QuestClustersAPI.all()) {
            Component name = Component.literal(c.title().isEmpty() ? QuestCluster.idToString(c.id()) : c.title());
            if (file.getChapter(c.chapterId()) == null) {
                out.add(issue(Level.WARN, "cluster_no_chapter", 0L, 0L, name));
                continue;
            }
            int gone = 0;
            for (long m : c.members()) if (file.getBase(m) == null) gone++;
            if (c.members().size() == gone) out.add(issue(Level.WARN, "empty_cluster", 0L, c.chapterId(), name));
            else if (gone > 0) out.add(issue(Level.INFO, "cluster_dangling", 0L, c.chapterId(), name, gone));
        }
    }

    private static void editorData(BaseQuestFile file, BookExtras book, Predicate<String> textureExists, List<Issue> out) {
        if (textureExists != null) {
            texture(textureExists, book.sidebarBackground, 0L, 0L, "missing_sidebar", out);
            for (var e : book.chapters.entrySet()) {
                texture(textureExists, e.getValue().background, e.getKey(), e.getKey(), "missing_chapter_background", out);
            }
            for (var e : book.quests.entrySet()) {
                long ch = chapterOf(file.getBase(e.getKey()));
                texture(textureExists, e.getValue().boxBackground(), e.getKey(), ch, "missing_quest_background", out);
                texture(textureExists, e.getValue().viewBackground(), e.getKey(), ch, "missing_quest_background", out);
            }
            for (LinePreset p : book.linePresets) {
                if (!p.texture().isEmpty() && !textureExists.test(p.texture())) {
                    out.add(issue(Level.ERROR, "missing_preset_image", 0L, 0L, p.name(), p.texture()));
                }
            }
            for (QuestCluster c : QuestClustersAPI.all()) {
                String bg = c.style().background();
                if (!bg.isEmpty() && !textureExists.test(bg)) {
                    out.add(issue(Level.ERROR, "missing_cluster_background", 0L, c.chapterId(), c.title(), bg));
                }
            }
        }
        for (var e : book.chapters.entrySet()) {
            ChapterExtras ce = e.getValue();
            if (!ce.linePreset.isEmpty() && book.preset(ce.linePreset) == null) {
                out.add(issue(Level.WARN, "missing_preset", e.getKey(), e.getKey(), ce.linePreset));
            }
            for (String id : ce.nodeLinePresets.values()) {
                if (!id.isEmpty() && book.preset(id) == null) out.add(issue(Level.WARN, "missing_preset", e.getKey(), e.getKey(), id));
            }
        }
    }

    private static void texture(Predicate<String> exists, String tex, long object, long chapter, String kind, List<Issue> out) {
        if (tex != null && !tex.isEmpty() && !exists.test(tex)) out.add(issue(Level.ERROR, kind, object, chapter, tex));
    }

    /** The texture a chapter image draws, if it is a plain texture (items, colours and the like have none). */
    private static String imageTexture(ChapterImage img) {
        // guide points, canvas text, GIFs and videos are drawn by the editor, not from the image
        if (img.getClickAction().actionData().startsWith(CanvasElement.PREFIX)) return null;
        String s = img.getImage().toString();
        if (s.isEmpty() || !s.contains(":") || s.startsWith("item:") || s.startsWith("#") || s.startsWith("http")) return null;
        int q = s.indexOf('?');
        if (q >= 0) s = s.substring(0, q);
        return s.endsWith(".png") || s.contains(":block/") || s.contains(":item/") || s.endsWith(".gif") ? s : null;
    }
}
