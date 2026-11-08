package com.questclusters.xmod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quest Clusters XMod: the FTB Quests part of FTB XMod Compat (item filters in tasks, KubeJS, JEI/REI, Jade, ...).
 * If FTB XMod Compat is installed too, its FTB Quests entry points are switched off (mixins in
 * {@code mixin/xmod}) and this module provides them; everything else stays with XMod.
 */
@Mod(QuestClustersXMod.MOD_ID)
public final class QuestClustersXMod {
    public static final String MOD_ID = "quest_clusters_xmod";
    public static final Logger LOGGER = LoggerFactory.getLogger("Quest Clusters XMod");

    public QuestClustersXMod(IEventBus modBus) {
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(QuestClustersXMod::setup));
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) ->
                    e.enqueueWork(com.questclusters.xmod.recipe.RecipeViewers::install));
        }
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient() && System.getProperty("quest_clusters.devtest") != null) {
            com.questclusters.xmod.client.XModDevTestAccess.register();
        }
        if (loaded("ftbxmodcompat")) {
            LOGGER.info("FTB XMod Compat is installed: taking over its FTB Quests integration (the rest of XMod is unchanged)");
        }
    }

    public static boolean loaded(String mod) {
        return ModList.get() != null && ModList.get().isLoaded(mod);
    }

    private static void setup() {
        if (loaded("ftbfiltersystem")) {
            com.questclusters.xmod.filtering.FfsFilterAdapter.register();
        }
    }
}
