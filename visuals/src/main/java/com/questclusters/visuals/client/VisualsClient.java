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

    /** /qcvisuals: FTB Library's config screen for the settings above. */
    public static void openSettings() {
        ConfigGroup group = new ConfigGroup("quest_clusters_visuals", accepted -> {
            if (accepted) VisualsConfig.save();
            Minecraft.getInstance().setScreen(null);
        });
        VisualsConfig.CONFIG.createClientConfig(group);
        new EditConfigScreen(group).openGui();
    }
}
