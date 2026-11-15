package com.questclusters.editor.client;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only setup. */
public final class EditorClient {
    private EditorClient() {
    }

    public static void init(net.neoforged.bus.api.IEventBus modBus) {
        com.questclusters.platform.Events.clientTickStart(com.questclusters.editor.perf.VisibilityCache::clear);
        // a resource pack may add or remove a decorative line texture
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent e) -> e.registerReloadListener(
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm ->
                        com.questclusters.editor.client.gui.DecorativeLinesWidget.resourcesReloaded()));
        modBus.addListener(com.questclusters.editor.client.paste.ConfigPacks::register);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RegisterClientCommandsEvent e) -> {
            for (String root : java.util.List.of("qceditor", "questenhance")) {
                e.getDispatcher().register(net.minecraft.commands.Commands.literal(root).then(net.minecraft.commands.Commands.literal("cleanup")
                        .executes(ctx -> {
                            ctx.getSource().sendSystemMessage(com.questclusters.editor.client.paste.PastedImages.cleanup());
                            return 1;
                        })));
            }
        });
        com.questclusters.editor.client.desc.DescriptionComponents.register();
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientEditor.clear());
        if (System.getProperty("quest_clusters.devtest") != null) {
            EditorDevTest.register();
        }
        if (Boolean.getBoolean("quest_clusters_editor.tour")) {
            EditorTour.register();
        }
    }

    /**
     * "Config" button in the mods list: NeoForge's screen for the client options. The internal flag that records the
     * one-time Quest Enhance import is not shown.
     */
    //? if forge {
    /*/// 1.20.1: no generated config screen; the same options in FTB Library's settings screen.
    public static net.minecraft.client.gui.screens.Screen settingsScreen(net.minecraft.client.gui.screens.Screen parent) {
        var group = new dev.ftb.mods.ftblibrary.config.ConfigGroup("quest_clusters_editor.configuration", accepted -> {
            if (accepted) com.questclusters.editor.EditorConfig.SPEC.save();
            net.minecraft.client.Minecraft.getInstance().tell(() -> net.minecraft.client.Minecraft.getInstance().setScreen(parent));
        });
        for (var e : java.util.List.of(
                java.util.Map.entry("render_kill_task_entity_models", com.questclusters.editor.EditorConfig.KILL_TASK_MODELS),
                java.util.Map.entry("open_advancement_tasks", com.questclusters.editor.EditorConfig.OPEN_ADVANCEMENTS),
                java.util.Map.entry("description_markup", com.questclusters.editor.EditorConfig.MARKUP),
                java.util.Map.entry("center_all_description_text", com.questclusters.editor.EditorConfig.CENTER_ALL),
                java.util.Map.entry("quest_visibility_cache", com.questclusters.editor.EditorConfig.VISIBILITY_CACHE),
                java.util.Map.entry("cull_offscreen_dependency_lines", com.questclusters.editor.EditorConfig.CULL_LINES))) {
            var value = e.getValue();
            group.addBool(e.getKey(), value.get(), value::set, value.getDefault());
        }
        var screen = new dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen(group);
        screen.setAutoclose(true);
        return new dev.ftb.mods.ftblibrary.ui.ScreenWrapper(screen);
    }
    *///?}

    //? if neoforge {
    public static void registerConfigScreen(net.neoforged.fml.ModContainer container) {
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (mod, parent) -> new net.neoforged.neoforge.client.gui.ConfigurationScreen(mod, parent,
                        (context, key, element) -> "imported_quest_enhance".equals(key) ? null : element));
    }
    //?}
}
