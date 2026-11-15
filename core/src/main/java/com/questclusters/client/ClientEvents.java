package com.questclusters.client;

import com.questclusters.QuestClusters;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftblibrary.util.client.ClientUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
//? if neoforge {
import net.neoforged.neoforge.client.event.ClientTickEvent;
//?}

@EventBusSubscriber(modid = QuestClusters.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static int seenRevision = -1;

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        ClusterViewState.onLogin();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClusterViewState.onLogout();
        ClientClusters.clear();
        ClientScoring.clear();
        ClientPersonal.clear();
        ClientLimits.clear();
        com.questclusters.client.gui.ClusterText.clearCache();
    }

    /** Rebuild the open quest panel when cluster data arrives from the server. */
    //? if neoforge {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
    //?} else {
    /*@SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
    *///?}
        ClusterClipboard.tick();
        ClientPersonal.tick();
        int rev = (ClientClusters.revision() * 31 + ClientScoring.revision()) * 31 + ClientPersonal.revision();
        if (rev != seenRevision) {
            seenRevision = rev;
            QuestScreen qs = ClientUtils.getCurrentGuiAs(QuestScreen.class);
            if (qs != null) {
                qs.refreshQuestPanel();
                qs.otherButtonsTopPanel.refreshWidgets(); // settlement / leaderboard buttons
            }
        }
    }
}
