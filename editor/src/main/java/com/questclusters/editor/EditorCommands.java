package com.questclusters.editor;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * {@code /questclusters persistent set <players> <key> <value>}: writes a string into the players' persistent data
 * (shown by description placeholders). Also registered as {@code /questenhance persistent set ...} so commands and
 * scripts written for Quest Enhance keep working.
 */
public final class EditorCommands {
    private EditorCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(root("questclusters"));
        d.register(root("questenhance"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name).then(Commands.literal("persistent").requires(s -> s.hasPermission(2))
                .then(Commands.literal("set").then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.argument("key", StringArgumentType.word())
                                .then(Commands.argument("value", StringArgumentType.greedyString()).executes(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
                                    String key = StringArgumentType.getString(ctx, "key");
                                    String value = StringArgumentType.getString(ctx, "value");
                                    players.forEach(p -> p.getPersistentData().putString(key, value));
                                    ctx.getSource().sendSuccess(() -> Component.translatable("quest_clusters_editor.persistent.set",
                                            key, players.size()), true);
                                    return players.size();
                                }))))));
    }
}
