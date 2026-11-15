package com.questclusters.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;

/** Game events the mods listen to, the same API on every loader. */
public final class Events {
    private Events() {
    }

    /** After every server tick. */
    public static void serverTick(Runnable r) {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> r.run());
    }

    /** Before every client tick. */
    public static void clientTickStart(Runnable r) {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Pre e) -> r.run());
    }

    /** After every client tick. */
    public static void clientTick(Runnable r) {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e) -> r.run());
    }

    /** After every rendered frame. */
    public static void frameRendered(Runnable r) {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Post e) -> r.run());
    }

    public static void commands(Consumer<CommandDispatcher<CommandSourceStack>> c) {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> c.accept(e.getDispatcher()));
    }

    /** Client-side commands (work without the mod on the server). */
    public static void clientCommands(Consumer<CommandDispatcher<CommandSourceStack>> c) {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RegisterClientCommandsEvent e) -> c.accept(e.getDispatcher()));
    }
}
