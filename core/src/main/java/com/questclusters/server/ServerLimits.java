package com.questclusters.server;

import com.questclusters.QuestClusters;
import com.questclusters.data.ClaimLimits;
import com.questclusters.net.ClusterPayloads;
import com.questclusters.platform.Ftbq;
import com.questclusters.platform.Net;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Server side of quest and reward limits. Rules live with the quest book
 * ({@code config/ftbquests/quests/quest_clusters/limits.snbt}); claim counts live with the world
 * ({@code <world>/data/quest_clusters_claims.snbt}).
 */
public final class ServerLimits {
    private static final String RULES_FILE = "limits.snbt";
    private static final ClaimLimits LIMITS = new ClaimLimits();
    private static boolean rulesDirty, countsDirty;

    private ServerLimits() {
    }

    public static ClaimLimits limits() {
        return LIMITS;
    }

    // ---------------------------------------------------------------- lifecycle (from ServerClusters)

    static void load(ServerQuestFile file) {
        LIMITS.clearRules();
        rulesDirty = false;
        Path p = rulesPath(file);
        if (p != null && Files.exists(p)) {
            SNBTCompoundTag tag = SNBT.read(p);
            if (tag != null) LIMITS.readRules(tag);
            else QuestClusters.LOGGER.error("Could not parse {}", p);
        }
        // drop rules of quests and rewards that are gone
        rulesDirty |= LIMITS.questRules().keySet().removeIf(id -> !(file.getBase(id) instanceof Quest));
        rulesDirty |= LIMITS.rewardRules().keySet().removeIf(id -> !(file.getBase(id) instanceof Reward));
        rulesDirty |= LIMITS.personalQuests().removeIf(id -> !(file.getBase(id) instanceof Quest));
        // counts: only on the first load of this server (a /ftbquests reload must not lose unsaved counts)
        Path c = countsPath();
        if (!countsLoaded) {
            LIMITS.clearCounts();
            countsDirty = false;
            if (c != null && Files.exists(c)) {
                SNBTCompoundTag tag = SNBT.read(c);
                if (tag != null) LIMITS.readCounts(tag);
                else QuestClusters.LOGGER.error("Could not parse {}", c);
            }
            countsLoaded = true;
        }
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s != null) s.getPlayerList().getPlayers().forEach(ServerLimits::sendTo);
    }

    private static boolean countsLoaded;

    static void save(ServerQuestFile file) {
        if (rulesDirty) {
            Path p = rulesPath(file);
            if (p != null) write(p, LIMITS.writeRules(SNBTCompoundTag::new));
            rulesDirty = false;
        }
        if (countsDirty) {
            Path c = countsPath();
            if (c != null) write(c, LIMITS.writeCounts(SNBTCompoundTag::new));
            countsDirty = false;
        }
    }

    static void unload() {
        LIMITS.clearRules();
        LIMITS.clearCounts();
        countsLoaded = false;
        rulesDirty = countsDirty = false;
    }

    private static void write(Path p, net.minecraft.nbt.CompoundTag tag) {
        try {
            Files.createDirectories(p.getParent());
            SNBT.write(p, (SNBTCompoundTag) tag);
        } catch (Exception e) {
            QuestClusters.LOGGER.error("Failed to save {}", p, e);
        }
    }

    private static Path rulesPath(ServerQuestFile file) {
        return file.getFolder() == null ? null : ClusterStorage.folder(file.getFolder()).resolve(RULES_FILE);
    }

    private static Path countsPath() {
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        return s == null ? null : s.getWorldPath(LevelResource.ROOT).resolve("data").resolve("quest_clusters_claims.snbt");
    }

    // ---------------------------------------------------------------- hooks (from the FTB Quests mixins)

    /**
     * A player claims a reward (by hand, "claim all" or auto-claim). A used-up reward is marked claimed without being
     * given, so the quest can still finish its round and repeat.
     *
     * @return true if the claim must not go ahead
     */
    public static boolean blockClaim(TeamData td, ServerPlayer player, Reward reward) {
        if (!LIMITS.exhausted(reward.id, td.getTeamId(), player.getUUID())) return false;
        ClaimLimits.RewardLimit l = LIMITS.reward(reward.id);
        player.displayClientMessage(Component.translatable("quest_clusters.limits.used_up", reward.getTitle(),
                l.player() > 0 && LIMITS.playerClaims(player.getUUID(), reward.id) >= l.player()
                        ? Component.translatable("quest_clusters.limits.by_player", l.player())
                        : Component.translatable("quest_clusters.limits.by_team", l.team())).withStyle(ChatFormatting.GOLD), false);
        return true;
    }

    /** A reward was given out. */
    public static void claimed(TeamData td, ServerPlayer player, Reward reward) {
        if (LIMITS.reward(reward.id).isEmpty()) return; // only limited rewards are counted
        LIMITS.countClaim(reward.id, td.getTeamId(), player.getUUID());
        countsDirty = true;
        td.getOnlineMembers().forEach(ServerLimits::sendTo);
    }

    /** May a repeatable quest reset for another round (both sides; the client knows the rules and its team's count)? */
    public static boolean mayRepeat(ClaimLimits limits, TeamData td, Quest quest) {
        return limits.mayRepeat(quest.id, td.getCompletionCount(quest));
    }

    // ---------------------------------------------------------------- editing

    public static void handleSetQuest(ServerPlayer player, long questId, int max) {
        edit(player, file -> {
            if (!(file.getBase(questId) instanceof Quest)) return;
            LIMITS.setQuestMax(questId, max);
        });
    }

    public static void handleSetReward(ServerPlayer player, long rewardId, ClaimLimits.RewardLimit limit) {
        edit(player, file -> {
            if (!(file.getBase(rewardId) instanceof Reward)) return;
            LIMITS.setReward(rewardId, limit);
        });
    }

    public static void handleSetPersonal(ServerPlayer player, long questId, boolean on) {
        edit(player, file -> {
            if (!(file.getBase(questId) instanceof Quest) || LIMITS.isPersonal(questId) == on) return;
            // teams get their own state back under the old rules before the set changes
            PersonalProgress.unloadAll(file);
            LIMITS.setPersonal(questId, on);
            PersonalProgress.rulesChanged(file);
        });
    }

    private static void edit(ServerPlayer player, java.util.function.Consumer<ServerQuestFile> action) {
        Ftbq.server().ifPresent(file -> {
            if (!Ftbq.canEdit(player)) {
                player.displayClientMessage(Component.translatable("quest_clusters.error.no_permission").withStyle(ChatFormatting.RED), false);
                return;
            }
            action.accept(file);
            rulesDirty = true;
            file.markDirty(); // FTB Quests saves soon, and our rules with it
            player.server.getPlayerList().getPlayers().forEach(ServerLimits::sendTo);
        });
    }

    /** Admin command: forget claim counts. Returns how many entries were removed. */
    public static int reset(UUID team, UUID player, long reward) {
        int n = LIMITS.resetCounts(team, player, reward);
        if (n > 0) {
            countsDirty = true;
            MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
            if (s != null) s.getPlayerList().getPlayers().forEach(ServerLimits::sendTo);
        }
        return n;
    }

    // ---------------------------------------------------------------- sync

    public static void sendTo(ServerPlayer player) {
        UUID team = Ftbq.server().map(f -> f.getOrCreateTeamData(player).getTeamId()).orElse(null);
        Net.sendToPlayer(player, new ClusterPayloads.LimitsSync(new java.util.LinkedHashMap<>(LIMITS.questRules()),
                new java.util.LinkedHashMap<>(LIMITS.rewardRules()),
                team == null ? java.util.Map.of() : new java.util.HashMap<>(LIMITS.teamCounts(team)),
                new java.util.HashMap<>(LIMITS.playerCounts(player.getUUID())), java.util.List.copyOf(LIMITS.personalQuests())));
    }
}
