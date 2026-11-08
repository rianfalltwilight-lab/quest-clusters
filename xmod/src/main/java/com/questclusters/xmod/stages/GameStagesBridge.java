package com.questclusters.xmod.stages;

import com.questclusters.xmod.QuestClustersXMod;
import dev.ftb.mods.ftblibrary.integration.stages.StageProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Game Stages has no official 1.21.1 release; "Game Stages Reborn" keeps its mod id and API
 * ({@code net.darkhax.gamestages.GameStageHelper}, {@code GameStageEvent}). Reached by reflection so no port is a
 * build dependency; anything missing is logged once and the integration stays off.
 */
final class GameStagesBridge {
    private static final String HELPER = "net.darkhax.gamestages.GameStageHelper";
    private static final String EVENTS = "net.darkhax.gamestages.event.GameStageEvent$";

    private GameStagesBridge() {
    }

    static @Nullable StageProvider provider() {
        try {
            Class<?> helper = Class.forName(HELPER);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodHandle has = lookup.findStatic(helper, "hasStage", MethodType.methodType(boolean.class, Player.class, String.class));
            MethodHandle add = lookup.findStatic(helper, "addStage", MethodType.methodType(void.class, ServerPlayer.class, String[].class));
            MethodHandle remove = lookup.findStatic(helper, "removeStage", MethodType.methodType(void.class, ServerPlayer.class, String[].class));
            MethodHandle sync = lookup.findStatic(helper, "syncPlayer", MethodType.methodType(void.class, ServerPlayer.class));
            return new StageProvider() {
                @Override
                public boolean has(Player player, String stage) {
                    try {
                        return (boolean) has.invokeExact(player, stage);
                    } catch (Throwable t) {
                        throw new IllegalStateException(t);
                    }
                }

                @Override
                public void add(ServerPlayer player, String stage) {
                    try {
                        add.invokeExact(player, new String[]{stage});
                    } catch (Throwable t) {
                        throw new IllegalStateException(t);
                    }
                }

                @Override
                public void remove(ServerPlayer player, String stage) {
                    try {
                        remove.invokeExact(player, new String[]{stage});
                    } catch (Throwable t) {
                        throw new IllegalStateException(t);
                    }
                }

                @Override
                public void sync(ServerPlayer player) {
                    try {
                        sync.invokeExact(player);
                    } catch (Throwable t) {
                        throw new IllegalStateException(t);
                    }
                }

                @Override
                public String getName() {
                    return "Game Stages";
                }
            };
        } catch (ReflectiveOperationException e) {
            QuestClustersXMod.LOGGER.warn("Game Stages is installed but its API was not found ({}); stages stay with FTB Library's default", e.toString());
            return null;
        }
    }

    /** Stage changes from Game Stages itself (its commands, other mods) re-check stage tasks at once. */
    @SuppressWarnings("unchecked")
    static void listen() {
        for (String name : new String[]{"Added", "Removed", "Cleared"}) {
            try {
                Class<?> type = Class.forName(EVENTS + name);
                if (PlayerEvent.class.isAssignableFrom(type)) {
                    NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (Class<PlayerEvent>) type,
                            e -> Stages.refresh(e.getEntity()));
                } else {
                    QuestClustersXMod.LOGGER.warn("Game Stages event {} is not a player event; not listening", type.getName());
                }
            } catch (ClassNotFoundException e) {
                QuestClustersXMod.LOGGER.warn("Game Stages event {} not found; stage tasks refresh once a second instead", name);
            }
        }
    }
}
