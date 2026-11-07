package com.questclusters.client.gui;

import com.questclusters.client.ClientClusters;
import com.questclusters.data.QuestCluster;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.ChatFormatting;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Search over the quest book for the chapter panel's search box: quest titles, subtitles, descriptions and task
 * names, plus cluster titles. Never reveals what the player can't see: hidden quests and chapters are skipped, and
 * quests whose details are hidden until startable only match by title. Editors search everything.
 * Pinyin matching (e.g. "shitou" for 石头) is used when Just Enough Characters is installed.
 */
public final class QuestSearch {
    public static final int MAX_RESULTS = 60;
    private static final Pattern FORMAT = Pattern.compile("&[0-9a-fk-orA-FK-OR]");

    /** What matched, best first. */
    public enum Field { TITLE, CLUSTER, SUBTITLE, TASK, DESCRIPTION }

    public record Result(Quest quest, QuestCluster cluster, Chapter chapter, Field field) {
    }

    private static String query = "";

    private QuestSearch() {
    }

    public static String query() {
        return query;
    }

    public static void setQuery(String q) {
        query = q == null ? "" : q;
    }

    public static boolean active() {
        return !query.isBlank();
    }

    public static List<Result> search(String raw) {
        List<Result> out = new ArrayList<>();
        String q = norm(raw).trim();
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        if (q.isEmpty() || file == null || file.selfTeamData == null) {
            return out;
        }
        TeamData td = file.selfTeamData;
        boolean edit = file.canEdit();
        List<List<Result>> byField = new ArrayList<>();
        for (int i = 0; i < Field.values().length; i++) byField.add(new ArrayList<>());

        file.forAllChapters(chapter -> {
            if (!edit && !chapter.isVisible(td)) {
                return;
            }
            for (Quest quest : chapter.getQuests()) {
                if (!edit && !quest.isVisible(td)) {
                    continue;
                }
                Field f = match(quest, q, edit || !quest.hideDetailsUntilStartable() || td.canStartTasks(quest));
                if (f != null) {
                    byField.get(f.ordinal()).add(new Result(quest, null, chapter, f));
                }
            }
            for (QuestCluster c : ClientClusters.store().byChapter(chapter.id)) {
                boolean anyVisible = edit || c.members().stream().anyMatch(id -> file.getBase(id) instanceof Quest qq && qq.isVisible(td));
                if (anyVisible && (matches(ClusterText.plain(c.title()), q) || matches(ClusterText.plain(c.subtitle()), q))) {
                    byField.get(Field.CLUSTER.ordinal()).add(new Result(null, c, chapter, Field.CLUSTER));
                }
            }
        });
        for (List<Result> l : byField) {
            for (Result r : l) {
                if (out.size() >= MAX_RESULTS) return out;
                out.add(r);
            }
        }
        return out;
    }

    private static Field match(Quest quest, String q, boolean details) {
        if (matches(quest.getTitle().getString(), q)) return Field.TITLE;
        if (!details) return null;
        if (matches(quest.getSubtitle().getString(), q)) return Field.SUBTITLE;
        for (Task t : quest.getTasks()) {
            if (matches(t.getTitle().getString(), q)) return Field.TASK;
        }
        for (String line : quest.getRawDescription()) {
            if (matches(line, q)) return Field.DESCRIPTION;
        }
        return null;
    }

    static String norm(String s) {
        if (s == null) return "";
        String plain = ChatFormatting.stripFormatting(FORMAT.matcher(s).replaceAll(""));
        return plain == null ? "" : plain.toLowerCase(Locale.ROOT);
    }

    static boolean matches(String text, String normQuery) {
        if (text == null || text.isEmpty()) return false;
        String n = norm(text);
        return n.contains(normQuery) || pinyin(n, normQuery);
    }

    // ---------------------------------------------------------------- optional pinyin via Just Enough Characters
    private static boolean pinyinInit;
    private static Method pinyinContains;

    private static boolean pinyin(String text, String q) {
        if (!pinyinInit) {
            pinyinInit = true;
            if (net.neoforged.fml.ModList.get().isLoaded("jecharacters")) {
                for (String cls : new String[]{"me.towdium.jecharacters.utils.Match", "me.towdium.jecharacters.JechMatch"}) {
                    try {
                        pinyinContains = Class.forName(cls).getMethod("contains", String.class, CharSequence.class);
                        break;
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        if (pinyinContains == null) return false;
        try {
            return (Boolean) pinyinContains.invoke(null, text, q);
        } catch (Throwable t) {
            pinyinContains = null;
            return false;
        }
    }
}
