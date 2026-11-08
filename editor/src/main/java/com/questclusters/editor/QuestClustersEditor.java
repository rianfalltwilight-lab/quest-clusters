package com.questclusters.editor;

import com.questclusters.api.LineData;
import com.questclusters.api.QuestBookHooks;
import com.questclusters.editor.migration.QuestEnhanceMigration;
import com.questclusters.editor.net.EditorNetwork;
import com.questclusters.editor.server.ServerEditor;
import com.questclusters.migration.MigrationRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quest Clusters Editor: quest book editing extras (backgrounds, canvas text, guide points, decorative and hidden
 * lines, chapter prerequisites, ...). Replaces Quest Enhance and imports its data once.
 */
@Mod(QuestClustersEditor.MOD_ID)
public final class QuestClustersEditor {
    public static final String MOD_ID = "quest_clusters_editor";
    public static final Logger LOGGER = LoggerFactory.getLogger("Quest Clusters Editor");

    public QuestClustersEditor(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, EditorConfig.SPEC);
        modBus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Loading e) -> {
            if (e.getConfig().getSpec() == EditorConfig.SPEC) EditorConfig.importQuestEnhance();
        });
        MigrationRegistry.register(new QuestEnhanceMigration());
        QuestBookHooks.register(ServerEditor.INSTANCE);
        LineData.Holder.set(new EditorLineData());
        modBus.addListener(EditorNetwork::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.RegisterCommandsEvent e) -> EditorCommands.register(e.getDispatcher()));
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            com.questclusters.editor.client.EditorClient.init(modBus);
        }
    }
}
