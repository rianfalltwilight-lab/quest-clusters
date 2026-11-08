package com.questclusters.visuals;

import com.questclusters.visuals.client.VisualsClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quest book visuals: hover animations, quest details slide-in, smooth zoom, entity icons, animated PNG (APNG)
 * images and per-chapter shader backgrounds. Behaviour-compatible replacement for Certain Questing Additions
 * (its settings, entity icon syntax and chapter_shaders.json resource-pack format keep working). Client only.
 */
@Mod(QuestClustersVisuals.MOD_ID)
public final class QuestClustersVisuals {
    public static final String MOD_ID = "quest_clusters_visuals";
    public static final Logger LOGGER = LoggerFactory.getLogger("Quest Clusters Visuals");

    public QuestClustersVisuals(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            VisualsClient.init(modBus);
            VisualsClient.registerConfigScreen(container);
        }
    }
}
