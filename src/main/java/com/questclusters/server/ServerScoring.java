package com.questclusters.server;

import com.questclusters.QuestClusters;
import com.questclusters.data.ScoreCalc;
import com.questclusters.data.ScoreEntry;
import com.questclusters.data.ScoringNbt;
import com.questclusters.data.SettlementSettings;
import com.questclusters.net.ClusterPayloads;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.integration.PermissionsHelper;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.history.ChangeType;
import dev.ftb.mods.ftbquests.quest.history.QuestBookEditEvent;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Server side of scoring: per-quest scores/nodes and settlement settings, stored in
 * {@code config/ftbquests/quests/quest_clusters/scoring.snbt}. Scores are never stored per team; they are computed
 * from FTB Quests' own completion data whenever needed.
 */
public final class ServerScoring {
    private static final String FILE = "scoring.snbt";
    private static final Map<Long, ScoreEntry> ENTRIES = new LinkedHashMap<>();
    private static SettlementSettings settings = SettlementSettings.DEFAULT;
    private static boolean dirty;
    /** Settlement pages to open shortly after a quest completes: player id → server tick to open at. */
    private static final Map<UUID, Integer> PENDING_OPEN = new LinkedHashMap<>();
    private static int ticks;

    private ServerScoring() {
    }

    public static Map<Long, ScoreEntry> entries() {
        return ENTRIES;
    }

    public static SettlementSettings settings() {
        return settings;
    }

    public static void init() {
        ObjectCompletedEvent.QUEST.register(event -> {
            ScoreEntry e = ENTRIES.get(event.getQuest().id);
            if (e != null && e.opensSettlement()) {
                for (ServerPlayer p : event.getOnlineMembers()) {
                    PENDING_OPEN.put(p.getUUID(), ticks + 40); // let the completion toast show first
                }
            }
            return dev.architectury.event.EventResult.pass();
        });
    }

    // ---------------------------------------------------------------- lifecycle (from ServerClusters)

    static void load(ServerQuestFile file) {
        ENTRIES.clear();
        settings = SettlementSettings.DEFAULT;
        dirty = false;
        Path p = path(file);
        if (p != null && Files.exists(p)) {
            SNBTCompoundTag tag = SNBT.read(p);
            if (tag != null) {
                ScoringNbt.readEntries(tag).forEach((id, e) -> {
                    if (file.getBase(id) instanceof Quest) {
                        ENTRIES.put(id, e);
                    } else {
                        dirty = true; // quest gone since last start: drop (undo history doesn't survive restarts)
                    }
                });
                settings = ScoringNbt.readSettings(tag);
            } else {
                QuestClusters.LOGGER.error("Could not parse {}", p);
            }
        }
        broadcast();
    }

    static void save(ServerQuestFile file) {
        if (!dirty) return;
        Path p = path(file);
        if (p == null) return;
        try {
            Files.createDirectories(p.getParent());
            SNBTCompoundTag tag = (SNBTCompoundTag) ScoringNbt.write(ENTRIES, settings, SNBTCompoundTag::new);
            SNBT.write(p, tag);
            dirty = false;
        } catch (Exception e) {
            QuestClusters.LOGGER.error("Failed to save {}", p, e);
        }
    }

    static void unload() {
        ENTRIES.clear();
        settings = SettlementSettings.DEFAULT;
        PENDING_OPEN.clear();
    }

    static void tick(MinecraftServer server) {
        ticks++;
        if (PENDING_OPEN.isEmpty()) return;
        PENDING_OPEN.entrySet().removeIf(e -> {
            if (e.getValue() > ticks) return false;
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null) PacketDistributor.sendToPlayer(p, new ClusterPayloads.OpenSettlement());
            return true;
        });
    }

    private static Path path(ServerQuestFile file) {
        return file.getFolder() == null ? null : ClusterStorage.folder(file.getFolder()).resolve(FILE);
    }

    // ---------------------------------------------------------------- sync

    public static void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ClusterPayloads.ScoringSync(new LinkedHashMap<>(ENTRIES), settings));
    }

    static void broadcast() {
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s != null) s.getPlayerList().getPlayers().forEach(ServerScoring::sendTo);
    }

    // ---------------------------------------------------------------- edits (undoable)

    public static void handleSetEntry(ServerPlayer player, long questId, ScoreEntry entry) {
        ServerQuestFile.getInstance().ifPresent(file -> {
            if (!PermissionsHelper.canPlayerEdit(player)) {
                player.displayClientMessage(Component.translatable("quest_clusters.error.no_permission").withStyle(ChatFormatting.RED), false);
                return;
            }
            if (!(file.getBase(questId) instanceof Quest) || entry.node().length() > ScoreEntry.MAX_NODE_LENGTH) {
                return;
            }
            ScoreEntry before = ENTRIES.get(questId);
            ScoreEntry after = entry.isEmpty() ? null : entry;
            if (!java.util.Objects.equals(before, after)) {
                file.getHistoryStack().addAndApply(file, new EntryChange(questId, before, after));
            }
        });
    }

    public static void handleSetSettings(ServerPlayer player, SettlementSettings s) {
        ServerQuestFile.getInstance().ifPresent(file -> {
            if (!PermissionsHelper.canPlayerEdit(player)) {
                player.displayClientMessage(Component.translatable("quest_clusters.error.no_permission").withStyle(ChatFormatting.RED), false);
                return;
            }
            if (s.intro().size() > SettlementSettings.MAX_LINES || s.outro().size() > SettlementSettings.MAX_LINES) return;
            if (!s.equals(settings)) {
                file.getHistoryStack().addAndApply(file, new SettingsChange(settings, s));
            }
        });
    }

    /** Scripts / API: set without history. */
    public static void setEntry(long questId, ScoreEntry e) {
        if (e == null || e.isEmpty()) ENTRIES.remove(questId);
        else ENTRIES.put(questId, e);
        dirty = true;
        ServerQuestFile.getInstance().ifPresent(ServerQuestFile::markDirty);
        broadcast();
    }

    record EntryChange(long questId, ScoreEntry before, ScoreEntry after) implements QuestBookEditEvent {
        @Override
        public boolean apply(ServerQuestFile file) {
            setEntry(questId, after);
            return true;
        }

        @Override
        public boolean applyUndo(ServerQuestFile file) {
            setEntry(questId, before);
            return true;
        }

        @Override
        public List<Component> description(BaseQuestFile file, ChangeType type) {
            return List.of(Component.translatable("quest_clusters.event.score", getTitle(file, questId)));
        }
    }

    record SettingsChange(SettlementSettings before, SettlementSettings after) implements QuestBookEditEvent {
        @Override
        public boolean apply(ServerQuestFile file) {
            set(after);
            return true;
        }

        @Override
        public boolean applyUndo(ServerQuestFile file) {
            set(before);
            return true;
        }

        private static void set(SettlementSettings s) {
            settings = s;
            dirty = true;
            ServerQuestFile.getInstance().ifPresent(ServerQuestFile::markDirty);
            broadcast();
        }

        @Override
        public List<Component> description(BaseQuestFile file, ChangeType type) {
            return List.of(Component.translatable("quest_clusters.event.settlement"));
        }
    }

    // ---------------------------------------------------------------- scores and leaderboard

    public static ScoreCalc.Progress progress(BaseQuestFile file, TeamData td) {
        return new ScoreCalc.Progress() {
            public boolean questExists(long id) {
                return file.getBase(id) instanceof Quest;
            }

            public boolean isCompleted(long id) {
                return file.getBase(id) instanceof Quest q && td.isCompleted(q);
            }

            public OptionalLong completedAt(long id) {
                return td.getCompletedTime(id).map(d -> OptionalLong.of(d.getTime())).orElse(OptionalLong.empty());
            }

            public long chapterOf(long id) {
                return file.getBase(id) instanceof Quest q ? q.getChapter().id : 0L;
            }
        };
    }

    public static List<ScoreCalc.Row> leaderboard(ServerQuestFile file, UUID viewerTeam) {
        List<ScoreCalc.Row> rows = new ArrayList<>();
        var teams = FTBTeamsAPI.api().isManagerLoaded() ? FTBTeamsAPI.api().getManager() : null;
        for (TeamData td : file.getAllTeamData()) {
            String name = td.getName();
            if (teams != null) {
                var team = teams.getTeamByID(td.getTeamId()).orElse(null);
                if (team == null) continue;
                // a player who joined a party still has an old personal team: count only the party
                if (team.isPlayerTeam() && teams.getTeamForPlayerID(team.getOwner()).map(t -> !t.getId().equals(team.getId())).orElse(false)) {
                    continue;
                }
                name = team.getName().getString();
            }
            int[] quests = {0};
            file.forAllQuests(q -> {
                if (td.isCompleted(q)) quests[0]++;
            });
            int score = ScoreCalc.compute(ENTRIES, progress(file, td)).total();
            if (quests[0] == 0 && score == 0 && !td.getTeamId().equals(viewerTeam)) continue;
            rows.add(new ScoreCalc.Row(name, quests[0], score, td.getTeamId().equals(viewerTeam)));
        }
        return ScoreCalc.sort(rows);
    }

    public static void handleLeaderboard(ServerPlayer player) {
        if (!settings.leaderboard()) return;
        ServerQuestFile.getInstance().ifPresent(file -> {
            UUID team = file.getOrCreateTeamData(player).getTeamId();
            List<ScoreCalc.Row> rows = leaderboard(file, team);
            PacketDistributor.sendToPlayer(player, new ClusterPayloads.Leaderboard(rows.size() > 200 ? rows.subList(0, 200) : rows));
        });
    }
}
