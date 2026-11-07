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
                        .executes(ctx -> leaderboard(ctx.getSource())))
                .then(Commands.literal("migrate")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("status").executes(ctx -> migrateStatus(ctx.getSource())))
                        .then(Commands.literal("backup").executes(ctx -> migrateBackup(ctx.getSource())))));
    }

    private static int migrateStatus(CommandSourceStack src) {
        var file = ServerQuestFile.getInstance().orElse(null);
        if (file == null || file.getFolder() == null) return 0;
        var applied = com.questclusters.migration.MigrationRunner.applied(file.getFolder());
        var all = com.questclusters.migration.MigrationRegistry.all();
        src.sendSuccess(() -> Component.translatable("quest_clusters.migrate.header", all.size()).withStyle(ChatFormatting.GOLD), false);
        for (var m : all) {
            boolean done = applied.containsKey(m.id());
            src.sendSuccess(() -> Component.literal((done ? "✔ " : "• ") + m.id() + " — " + m.description())
                    .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        }
        var last = ServerClusters.lastMigration;
        if (last != null && last.backup() != null) {
            src.sendSuccess(() -> Component.translatable("quest_clusters.migrate.backup", last.backup().getFileName().toString()), false);
        }
        return all.size();
    }

    private static int migrateBackup(CommandSourceStack src) {
        var file = ServerQuestFile.getInstance().orElse(null);
        if (file == null || file.getFolder() == null) return 0;
        try {
            file.saveNow();
            var zip = com.questclusters.migration.MigrationRunner.backup(file.getFolder(), System.currentTimeMillis(), "manual");
            src.sendSuccess(() -> Component.translatable("quest_clusters.migrate.backup", zip.getFileName().toString()), true);
            return 1;
        } catch (java.io.IOException e) {
            src.sendFailure(Component.literal(e.toString()));
            return 0;
        }
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
