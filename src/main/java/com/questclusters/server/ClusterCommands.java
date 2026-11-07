package com.questclusters.server;

import com.mojang.brigadier.CommandDispatcher;
import com.questclusters.data.ScoreCalc;
import com.questclusters.net.ClusterPayloads;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.List;

/**
 * /questclusters settlement            open your settlement page
 * /questclusters settlement &lt;players&gt;  open it for others (op)
 * /questclusters leaderboard           top 10 in chat
 */
public final class ClusterCommands {
    private ClusterCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("questclusters")
                .then(Commands.literal("settlement")
                        .executes(ctx -> open(List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("players", EntityArgument.players())
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> open(EntityArgument.getPlayers(ctx, "players")))))
                .then(Commands.literal("leaderboard")
                        .executes(ctx -> leaderboard(ctx.getSource()))));
    }

    private static int open(Collection<ServerPlayer> players) {
        players.forEach(p -> PacketDistributor.sendToPlayer(p, new ClusterPayloads.OpenSettlement()));
        return players.size();
    }

    private static int leaderboard(CommandSourceStack src) {
        if (!ServerScoring.settings().leaderboard()) {
            src.sendFailure(Component.translatable("quest_clusters.leaderboard.disabled"));
            return 0;
        }
        var file = ServerQuestFile.getInstance().orElse(null);
        if (file == null) return 0;
        var viewer = src.getPlayer() != null ? file.getOrCreateTeamData(src.getPlayer()).getTeamId() : null;
        List<ScoreCalc.Row> rows = ServerScoring.leaderboard(file, viewer);
        src.sendSuccess(() -> Component.translatable("quest_clusters.leaderboard.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (int i = 0; i < Math.min(10, rows.size()); i++) {
            ScoreCalc.Row r = rows.get(i);
            int rank = i + 1;
            src.sendSuccess(() -> Component.translatable("quest_clusters.leaderboard.row", rank, r.team(), r.quests(), r.score())
                    .withStyle(r.own() ? ChatFormatting.YELLOW : ChatFormatting.WHITE), false);
        }
        if (rows.isEmpty()) {
            src.sendSuccess(() -> Component.translatable("quest_clusters.leaderboard.empty").withStyle(ChatFormatting.GRAY), false);
        }
        return rows.size();
    }
}
