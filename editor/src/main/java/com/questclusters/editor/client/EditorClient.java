package com.questclusters.editor.client;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only setup. */
public final class EditorClient {
    private EditorClient() {
    }

    public static void init(net.neoforged.bus.api.IEventBus modBus) {
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
}
