package com.questclusters.xmod;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quest Clusters XMod — FTB Quests cross-mod integration: JEI/REI quest lookups, Jade, item filters in tasks, GameStages, KubeJS events.
 * Placeholder until phase G; registers nothing yet.
 */
@Mod(QuestClustersXMod.MOD_ID)
public final class QuestClustersXMod {
    public static final String MOD_ID = "quest_clusters_xmod";
    public static final Logger LOGGER = LoggerFactory.getLogger("Quest Clusters XMod");

    public QuestClustersXMod() {
        LOGGER.info("{} loaded (no features yet: phase G)", MOD_ID);
    }
}
