package com.questclusters.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** {@code quest_clusters/scoring.snbt}: per-quest scores/nodes and the settlement page settings. */
public final class ScoringNbt {
    private ScoringNbt() {
    }

    public static CompoundTag write(Map<Long, ScoreEntry> entries, SettlementSettings s, Supplier<CompoundTag> factory) {
        CompoundTag root = factory.get();
        root.putInt("version", 1);
        ListTag quests = new ListTag();
        for (Map.Entry<Long, ScoreEntry> e : entries.entrySet()) {
            ScoreEntry v = e.getValue();
            CompoundTag t = factory.get();
            t.putString("quest", QuestCluster.idToString(e.getKey()));
            if (v.score() != 0) t.putInt("score", v.score());
            if (v.isNode()) t.putString("node", v.node());
            if (v.opensSettlement()) t.putBoolean("opens_settlement", true);
            quests.add(t);
        }
        root.put("quests", quests);
        CompoundTag st = factory.get();
        st.putString("title", s.title());
        st.put("intro", strings(s.intro()));
        st.put("outro", strings(s.outro()));
        st.put("ranks", strings(s.rankLines()));
        st.putBoolean("show_max", s.showMax());
        st.putBoolean("leaderboard", s.leaderboard());
        root.put("settlement", st);
        return root;
    }

    public static Map<Long, ScoreEntry> readEntries(CompoundTag root) {
        Map<Long, ScoreEntry> out = new LinkedHashMap<>();
        ListTag list = root.getList("quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            long id = QuestCluster.idFromString(t.getString("quest"));
            ScoreEntry e = new ScoreEntry(t.getInt("score"), t.getString("node"), t.getBoolean("opens_settlement"));
            if (id != 0L && !e.isEmpty()) out.put(id, e);
        }
        return out;
    }

    public static SettlementSettings readSettings(CompoundTag root) {
        if (!root.contains("settlement", Tag.TAG_COMPOUND)) {
            return SettlementSettings.DEFAULT;
        }
        CompoundTag st = root.getCompound("settlement");
        return new SettlementSettings(st.getString("title"), list(st, "intro"), list(st, "outro"),
                st.contains("ranks") ? SettlementSettings.parseRanks(list(st, "ranks")) : SettlementSettings.DEFAULT.ranks(),
                !st.contains("show_max") || st.getBoolean("show_max"),
                !st.contains("leaderboard") || st.getBoolean("leaderboard"));
    }

    private static ListTag strings(List<String> l) {
        ListTag t = new ListTag();
        l.forEach(s -> t.add(StringTag.valueOf(s)));
        return t;
    }

    private static List<String> list(CompoundTag t, String key) {
        List<String> out = new ArrayList<>();
        ListTag l = t.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) out.add(l.getString(i));
        return out;
    }
}
