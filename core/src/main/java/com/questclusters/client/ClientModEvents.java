package com.questclusters.client;

import com.questclusters.QuestClusters;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/** Client events on the mod bus (NeoForge routes them here by event type). */
@EventBusSubscriber(modid = QuestClusters.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    /** Texture sizes are cached; a resource pack change can swap the files behind them. */
    @SubscribeEvent
    public static void reloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm ->
                Textures.clearCache());
    }
}
