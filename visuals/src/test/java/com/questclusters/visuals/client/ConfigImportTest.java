package com.questclusters.visuals.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigImportTest {
    @Test
    void importsOnceAndNeverOverwrites(@TempDir Path tmp) throws Exception {
        Path cqa = tmp.resolve("certain_questing_additions/client-config.snbt");
        Path ours = tmp.resolve("quest_clusters_visuals/client-config.snbt");
        assertFalse(VisualsConfig.importFromCqa(cqa, ours), "nothing to import");
        Files.createDirectories(cqa.getParent());
        Files.writeString(cqa, "{ appearance: { quest_hover_glow_alpha: 77 } }");
        assertTrue(VisualsConfig.importFromCqa(cqa, ours));
        assertTrue(Files.readString(ours).contains("77"));
        assertTrue(Files.exists(cqa), "original left in place");
        Files.writeString(cqa, "{ appearance: { quest_hover_glow_alpha: 5 } }");
        assertFalse(VisualsConfig.importFromCqa(cqa, ours), "existing settings are never overwritten");
        assertTrue(Files.readString(ours).contains("77"));
    }
}
