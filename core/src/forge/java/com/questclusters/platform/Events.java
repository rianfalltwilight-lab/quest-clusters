package com.questclusters.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.function.Consumer;

/** Game events the mods listen to, the same API on every loader. */
public final class Events {
    private Events() {
    }

    /** After every server tick. */
    public static void serverTick(Runnable r) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) r.run();
        });
    }

    /** Before every client tick. */
    public static void clientTickStart(Runnable r) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.START) r.run();
        });
    }

    /** After every client tick. */
    public static void clientTick(Runnable r) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) r.run();
        });
    }

    /** After every rendered frame. */
    public static void frameRendered(Runnable r) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.RenderTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) r.run();
        });
    }

    public static void commands(Consumer<CommandDispatcher<CommandSourceStack>> c) {
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.RegisterCommandsEvent e) -> c.accept(e.getDispatcher()));
    }

    /** Client-side commands (work without the mod on the server). */
    public static void clientCommands(Consumer<CommandDispatcher<CommandSourceStack>> c) {
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.RegisterClientCommandsEvent e) -> c.accept(e.getDispatcher()));
    }
}
