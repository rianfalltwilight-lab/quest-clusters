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
                           Map<Long, Integer> teamCounts, Map<Long, Integer> playerCounts) {
        LIMITS.clearRules();
        quests.forEach(LIMITS::setQuestMax);
        rewards.forEach(LIMITS::setReward);
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
        int max = LIMITS.questMax(quest.id);
        var td = dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE != null ? dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE.selfTeamData : null;
        if (max == 0 || td == null || !quest.canBeRepeated()) return;
        int done = Math.min(max, td.getCompletionCount(quest) + (td.isCompleted(quest) ? 1 : 0));
        out.add(Component.translatable(done >= max ? "quest_clusters.limits.tooltip.quest_done" : "quest_clusters.limits.tooltip.quest", done, max)
                .withStyle(done >= max ? ChatFormatting.RED : ChatFormatting.GOLD));
    }

    public static boolean usedUp(long reward) {
        return team != null && LIMITS.exhausted(reward, team, player);
    }
}
