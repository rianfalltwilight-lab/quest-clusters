package com.questclusters.editor.client;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only setup. */
public final class EditorClient {
    private EditorClient() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientEditor.clear());
        if (System.getProperty("quest_clusters.devtest") != null) {
            EditorDevTest.register();
        }
        if (Boolean.getBoolean("quest_clusters_editor.tour")) {
            EditorTour.register();
        }
    }
}
