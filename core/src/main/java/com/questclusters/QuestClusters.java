package com.questclusters;

import com.questclusters.net.ClusterNetwork;
import com.questclusters.server.ServerClusters;
import com.questclusters.platform.Events;
import com.questclusters.platform.ModContext;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(QuestClusters.MOD_ID)
public final class QuestClusters {
    public static final String MOD_ID = "quest_clusters";
    public static final Logger LOGGER = LoggerFactory.getLogger("QuestClusters");

    //? if neoforge {
    public QuestClusters(net.neoforged.bus.api.IEventBus modBus, net.neoforged.fml.ModContainer container) {
        this(new ModContext(modBus, container));
    }
    //?} else {
    /*public QuestClusters() {
        this(ModContext.current());
    }
    *///?}

    private QuestClusters(ModContext ctx) {
        com.questclusters.api.QuestBookHooks.register(com.questclusters.server.MigrationNotice.INSTANCE);
        ClusterNetwork.register(ctx);
        com.questclusters.task.ClusterTask.init(); // register the "Complete quest cluster" task type early
        com.questclusters.server.ServerScoring.init();
        if (System.getProperty("quest_clusters.devtest") != null) {
            com.questclusters.migration.MigrationRegistry.register(new com.questclusters.migration.DevProbeMigration());
        }
        Events.commands(com.questclusters.server.ClusterCommands::register);
        Events.serverTick(ServerClusters::tick);
    }
}
