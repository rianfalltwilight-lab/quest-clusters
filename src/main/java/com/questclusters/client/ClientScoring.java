package com.questclusters.client;

import com.questclusters.client.gui.LeaderboardScreen;
import com.questclusters.client.gui.SettlementScreen;
import com.questclusters.data.ScoreCalc;
import com.questclusters.data.ScoreEntry;
import com.questclusters.data.SettlementSettings;
import com.questclusters.server.ServerScoring;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client copy of scoring data, and the entry points to the settlement and leaderboard screens. */
public final class ClientScoring {
    private static Map<Long, ScoreEntry> entries = Map.of();
    private static SettlementSettings settings = SettlementSettings.DEFAULT;
    private static int revision;

    private ClientScoring() {
    }

    public static Map<Long, ScoreEntry> entries() {
        return entries;
    }

    public static SettlementSettings settings() {
        return settings;
    }

    public static int revision() {
        return revision;
    }

    public static ScoreEntry entry(long questId) {
        return entries.getOrDefault(questId, new ScoreEntry(0, "", false));
    }

    public static boolean anyScoring() {
        return !entries.isEmpty();
    }

    public static void set(Map<Long, ScoreEntry> e, SettlementSettings s) {
        entries = new LinkedHashMap<>(e);
        settings = s;
        revision++;
    }

    public static void clear() {
        entries = Map.of();
        settings = SettlementSettings.DEFAULT;
        revision++;
    }

    /** This player's team result, computed locally from FTB Quests' client progress. */
    public static ScoreCalc.Result myResult() {
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        if (file == null || file.selfTeamData == null) {
            return new ScoreCalc.Result(0, 0, Map.of(), List.of(), 0);
        }
        return ScoreCalc.compute(entries, ServerScoring.progress(file, file.selfTeamData));
    }

    public static void openSettlement() {
        Minecraft.getInstance().setScreen(new SettlementScreen(Minecraft.getInstance().screen));
    }

    public static void showLeaderboard(List<ScoreCalc.Row> rows) {
        var mc = Minecraft.getInstance();
        var prev = mc.screen instanceof LeaderboardScreen ls ? ls.previous() : mc.screen;
        mc.setScreen(new LeaderboardScreen(prev, rows));
    }
}
