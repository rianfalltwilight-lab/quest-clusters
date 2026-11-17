package com.questclusters.client;

import com.questclusters.data.ClaimLimits;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The limit rules and this player's own claim counts, as the server sent them. */
public final class ClientLimits {
    private static final ClaimLimits LIMITS = new ClaimLimits();
    private static UUID team, player;

    private ClientLimits() {
    }

    public static ClaimLimits limits() {
        return LIMITS;
    }

    public static void set(Map<Long, Integer> quests, Map<Long, ClaimLimits.RewardLimit> rewards,
                           Map<Long, Integer> teamCounts, Map<Long, Integer> playerCounts, List<Long> personal,
                           Map<Long, Integer> batch) {
        boolean personalChanged = !new java.util.HashSet<>(personal).equals(LIMITS.personalQuests());
        LIMITS.clearRules();
        personal.forEach(id -> LIMITS.setPersonal(id, true));
        if (personalChanged) ClientPersonal.rulesChanged();
        quests.forEach(LIMITS::setQuestMax);
        rewards.forEach(LIMITS::setReward);
        batch.forEach(LIMITS::setBatch);
        var td = dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE != null ? dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE.selfTeamData : null;
        var mc = net.minecraft.client.Minecraft.getInstance();
        team = td != null ? td.getTeamId() : new UUID(0L, 0L);
        player = mc.player != null ? mc.player.getUUID() : new UUID(0L, 0L);
        LIMITS.setOwnCounts(team, teamCounts, player, playerCounts);
    }

    public static void clear() {
        LIMITS.clearRules();
        LIMITS.clearCounts();
    }

    /** Tooltip lines for a reward with limits: claims used so far, or that it is used up. */
    public static void rewardTooltip(long reward, List<Component> out) {
        ClaimLimits.RewardLimit l = LIMITS.reward(reward);
        if (l.isEmpty() || team == null) return;
        if (LIMITS.exhausted(reward, team, player)) {
            out.add(Component.translatable("quest_clusters.limits.tooltip.used_up").withStyle(ChatFormatting.RED));
        }
        if (l.team() > 0) {
            out.add(Component.translatable("quest_clusters.limits.tooltip.team", LIMITS.teamClaims(team, reward), l.team()).withStyle(ChatFormatting.GOLD));
        }
        if (l.player() > 0) {
            out.add(Component.translatable("quest_clusters.limits.tooltip.player", LIMITS.playerClaims(player, reward), l.player()).withStyle(ChatFormatting.GOLD));
        }
    }

    /** Tooltip line for a quest with a completion limit: rounds done out of the maximum. */
    public static void questTooltip(dev.ftb.mods.ftbquests.quest.Quest quest, List<Component> out) {
        if (LIMITS.isPersonal(quest.id)) out.add(Component.translatable("quest_clusters.personal.tooltip").withStyle(ChatFormatting.AQUA));
        batchTooltip(quest, out);
        int max = LIMITS.questMax(quest.id);
        var td = dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE != null ? dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE.selfTeamData : null;
        if (max == 0 || td == null || !quest.canBeRepeated()) return;
        int done = Math.min(max, td.getCompletionCount(quest) + (td.isCompleted(quest) ? 1 : 0));
        out.add(Component.translatable(done >= max ? "quest_clusters.limits.tooltip.quest_done" : "quest_clusters.limits.tooltip.quest", done, max)
                .withStyle(done >= max ? ChatFormatting.RED : ChatFormatting.GOLD));
    }

    /** Rounds a batch submit of this quest could run now (0 if it is off, or not possible right now). */
    public static int batchRounds(dev.ftb.mods.ftbquests.quest.Quest quest) {
        int max = LIMITS.batchMax(quest.id);
        var file = dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (max == 0 || file == null || file.selfTeamData == null || mc.player == null
                || !com.questclusters.server.ServerBatch.eligible(quest)) return 0;
        var td = file.selfTeamData;
        if (td.isCompleted(quest) || !td.canStartTasks(quest)) return 0;
        return com.questclusters.server.ServerBatch.affordableRounds(quest, td, mc.player.getInventory().items, max);
    }

    /** Shift-click on a task: asks the server for a batch submit if the quest allows one. True if the click was used. */
    public static boolean batchClick(dev.ftb.mods.ftbquests.quest.task.Task task) {
        var quest = task.getQuest();
        if (LIMITS.batchMax(quest.id) == 0 || !com.questclusters.server.ServerBatch.eligible(quest)) return false;
        com.questclusters.platform.Net.sendToServer(new com.questclusters.net.ClusterPayloads.BatchSubmit(quest.id));
        return true;
    }

    /** Tooltip line for a task of a batch-submit quest: how to use it and how many rounds the items allow. */
    public static void batchTooltip(dev.ftb.mods.ftbquests.quest.Quest quest, List<Component> out) {
        int max = LIMITS.batchMax(quest.id);
        if (max == 0 || !com.questclusters.server.ServerBatch.eligible(quest)) return;
        out.add(Component.translatable("quest_clusters.batch.tooltip", batchRounds(quest), max).withStyle(ChatFormatting.AQUA));
    }

    public static boolean usedUp(long reward) {
        return team != null && LIMITS.exhausted(reward, team, player);
    }
}
