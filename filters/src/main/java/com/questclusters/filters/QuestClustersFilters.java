package com.questclusters.filters;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quest Clusters Filters — Extra smart filters for FTB Filter System: item lists, multiple item tags, block tags, fuel value.
 * Placeholder until phase F; registers nothing yet.
 */
@Mod(QuestClustersFilters.MOD_ID)
public final class QuestClustersFilters {
    public static final String MOD_ID = "quest_clusters_filters";
    public static final Logger LOGGER = LoggerFactory.getLogger("Quest Clusters Filters");

    public QuestClustersFilters() {
        LOGGER.info("{} loaded (no features yet: phase F)", MOD_ID);
    }
}
