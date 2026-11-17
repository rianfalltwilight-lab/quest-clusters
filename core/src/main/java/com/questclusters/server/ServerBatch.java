package com.questclusters.server;

import com.questclusters.data.ClaimLimits;
import com.questclusters.platform.Ftbq;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Batch submit: one shift-click submits a repeatable quest's items and finishes it round after round, as long as the
 * player has the items for a whole round and nothing else stops it (cooldown, completion limit, a used-up reward, a
 * reward that must be claimed by hand).
 */
public final class ServerBatch {
    /** Set while a batch runs past its first round: FTB Quests' completion toasts are left out (one summary instead). */
    private static boolean quiet;

    private ServerBatch() {
    }

    public static boolean quiet() {
        return quiet;
    }

    /**
     * Whether a quest can be batch submitted at all: repeatable, with at least one task, and every task one that takes
     * items (a task that only looks at the inventory would hand out rewards for nothing, round after round).
     */
    public static boolean eligible(Quest quest) {
        if (!quest.canBeRepeated() || quest.getTasks().isEmpty()) return false;
        for (Task t : quest.getTasks()) {
            if (!(t instanceof ItemTask it) || !it.consumesResources()) return false;
        }
        return true;
    }

    /**
     * How many whole rounds the items allow (at most {@code limit}). The current round counts what has been submitted
     * already; items that several tasks accept are only counted once.
     */
    public static int affordableRounds(Quest quest, TeamData td, List<ItemStack> inventory, int limit) {
        int[] left = new int[inventory.size()];
        for (int i = 0; i < left.length; i++) left[i] = inventory.get(i).getCount();
        for (int round = 0; round < limit; round++) {
            for (Task t : quest.getTasks()) {
                ItemTask task = (ItemTask) t;
                long need = round == 0 ? Math.max(0L, task.getMaxProgress() - td.getProgress(task)) : task.getMaxProgress();
                for (int i = 0; i < left.length && need > 0; i++) {
                    if (left[i] <= 0 || !task.test(inventory.get(i))) continue;
                    int take = (int) Math.min(need, left[i]);
                    left[i] -= take;
                    need -= take;
                }
                if (need > 0) return round;
            }
        }
        return limit;
    }

    // ---------------------------------------------------------------- server

    public static void handle(ServerPlayer player, long questId) {
        Ftbq.server().ifPresent(file -> {
            if (!(file.getBase(questId) instanceof Quest quest)) return;
            int max = ServerLimits.limits().batchMax(questId);
            if (max == 0 || !eligible(quest)) {
                player.displayClientMessage(Component.translatable("quest_clusters.batch.not_allowed").withStyle(ChatFormatting.RED), false);
                return;
            }
            TeamData td = file.getOrCreateTeamData(player); // a personal quest's state is the player's own from here on
            if (td.isLocked()) return;
            file.withPlayerContext(player, () -> run(player, td, quest, max));
        });
    }

    private static void run(ServerPlayer player, TeamData td, Quest quest, int max) {
        ClaimLimits limits = ServerLimits.limits();
        int rounds = 0;
        Component stop;
        try {
            while (true) {
                if (rounds >= max) {
                    stop = Component.translatable("quest_clusters.batch.stop.max", max);
                    break;
                }
                if (td.isCompleted(quest)) {
                    // the last round did not reset: its completion limit is reached, or a reward waits to be claimed by hand
                    stop = Component.translatable(ServerLimits.mayRepeat(limits, td, quest) ? "quest_clusters.batch.stop.unclaimed"
                            : "quest_clusters.batch.stop.completion_limit");
                    break;
                }
                if (!td.canStartTasks(quest)) {
                    stop = Component.translatable("quest_clusters.batch.stop.cannot_start");
                    break;
                }
                Reward usedUp = quest.getRewards().stream()
                        .filter(r -> limits.exhausted(r.id, td.getTeamId(), player.getUUID())).findFirst().orElse(null);
                if (usedUp != null) {
                    stop = Component.translatable("quest_clusters.batch.stop.used_up", usedUp.getTitle());
                    break;
                }
                if (affordableRounds(quest, td, player.getInventory().items, 1) == 0) {
                    stop = Component.translatable("quest_clusters.batch.stop.items");
                    break;
                }
                for (Task t : quest.getTasks()) {
                    if (!td.isCompleted(t)) t.submitTask(td, player);
                }
                if (!td.isCompleted(quest)) {
                    stop = Component.translatable("quest_clusters.batch.stop.items"); // a task took less than it should have
                    break;
                }
                rounds++;
                quiet = true; // first round's completion was shown; the rest are summed up below
                for (Reward r : List.copyOf(quest.getRewards())) {
                    // like FTB Quests' "claim all": rewards marked to be claimed by hand (and choices) are left alone
                    if (!r.getExcludeFromClaimAll()) td.claimReward(player, r, rounds == 1);
                }
            }
        } finally {
            quiet = false;
        }
        player.displayClientMessage(rounds == 0
                ? Component.translatable("quest_clusters.batch.none", stop).withStyle(ChatFormatting.GOLD)
                : Component.translatable("quest_clusters.batch.done", rounds, quest.getTitle(), stop).withStyle(ChatFormatting.GREEN), false);
    }
}
