package com.questclusters.editor;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quest Clusters Editor — Quest book editing extras: pasted images, canvas text, decorative and hidden lines, description components.
 * Placeholder until phase E; registers nothing yet.
 */
@Mod(QuestClustersEditor.MOD_ID)
public final class QuestClustersEditor {
    public static final String MOD_ID = "quest_clusters_editor";
    public static final Logger LOGGER = LoggerFactory.getLogger("Quest Clusters Editor");

    public QuestClustersEditor() {
        LOGGER.info("{} loaded (no features yet: phase E)", MOD_ID);
    }
}
