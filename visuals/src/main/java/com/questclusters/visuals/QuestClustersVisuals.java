package com.questclusters.visuals;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quest Clusters Visuals — Quest book animations, hover effects, smooth zoom, animated images and chapter backgrounds.
 * Placeholder until phase D; registers nothing yet.
 */
@Mod(QuestClustersVisuals.MOD_ID)
public final class QuestClustersVisuals {
    public static final String MOD_ID = "quest_clusters_visuals";
    public static final Logger LOGGER = LoggerFactory.getLogger("Quest Clusters Visuals");

    public QuestClustersVisuals() {
        LOGGER.info("{} loaded (no features yet: phase D)", MOD_ID);
    }
}
