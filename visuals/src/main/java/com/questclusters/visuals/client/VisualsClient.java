package com.questclusters.visuals.client;

import com.questclusters.api.VisualSettings;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client wiring of the visuals module. */
public final class VisualsClient {
    private VisualsClient() {
    }

    public static void init(IEventBus modBus) {
        VisualsConfig.load();
        if (System.getProperty("quest_clusters.devtest") != null) {
            VisualsDevTest.register();
        }
        VisualSettings.Holder.set(new VisualSettings() {
            public boolean questHover() {
                return VisualsConfig.QUEST_HOVER.get();
            }

            public int questHoverGlowAlpha() {
                return VisualsConfig.QUEST_HOVER_GLOW_ALPHA.get();
            }

            public boolean smoothAnimations() {
                return VisualsConfig.SMOOTH_ANIMATIONS.get();
            }
        });
        modBus.addListener(ChapterShaders::registerShaders);
        // decoded animations and entity models are rebuilt after a resource reload
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent e) -> e.registerReloadListener(
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> {
                    ApngTextures.clear();
                    EntityIcon.clearCache();
                }));
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent e) -> e.getDispatcher().register(
                Commands.literal("qcvisuals").executes(ctx -> {
                    Minecraft.getInstance().tell(VisualsClient::openSettings);
                    return 1;
                })));
    }

    /** /qcvisuals: FTB Library's config screen for the settings above; closes to the game. */
    public static void openSettings() {
        Minecraft.getInstance().setScreen(settingsScreen(null));
    }

    /** "Config" button in the mods list: the same screen, returning to the mods list. */
    public static void registerConfigScreen(net.neoforged.fml.ModContainer container) {
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (mod, parent) -> settingsScreen(parent));
    }

    /** Accept saves, Cancel discards; either way the screen goes back to {@code parent} (null: the game). */
    static net.minecraft.client.gui.screens.Screen settingsScreen(@org.jetbrains.annotations.Nullable net.minecraft.client.gui.screens.Screen parent) {
        ConfigGroup group = new ConfigGroup("quest_clusters_visuals", accepted -> {
            if (accepted) VisualsConfig.save();
        });
        VisualsConfig.CONFIG.createClientConfig(group);
        EditConfigScreen screen = new EditConfigScreen(group);
        screen.setPreviousScreen(parent);
        screen.setAutoclose(true).setOpenPrevScreenOnClose(true);
        return new dev.ftb.mods.ftblibrary.ui.ScreenWrapper(screen);
    }
}
