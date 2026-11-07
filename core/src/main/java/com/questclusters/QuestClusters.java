package com.questclusters;

import com.questclusters.net.ClusterNetwork;
import com.questclusters.server.ServerClusters;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(QuestClusters.MOD_ID)
public final class QuestClusters {
    public static final String MOD_ID = "quest_clusters";
    public static final Logger LOGGER = LoggerFactory.getLogger("QuestClusters");

    public QuestClusters(IEventBus modBus) {
        modBus.addListener(ClusterNetwork::register);
        com.questclusters.task.ClusterTask.init(); // register the "Complete quest cluster" task type early
        com.questclusters.server.ServerScoring.init();
        if (System.getProperty("quest_clusters.devtest") != null) {
            com.questclusters.migration.MigrationRegistry.register(new com.questclusters.migration.DevProbeMigration());
        }
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> com.questclusters.server.ClusterCommands.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> ServerClusters.tick());
    }
}
