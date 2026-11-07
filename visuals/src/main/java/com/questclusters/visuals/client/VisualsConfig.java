package com.questclusters.visuals.client;

import com.questclusters.visuals.QuestClustersVisuals;
import dev.ftb.mods.ftblibrary.snbt.config.BooleanValue;
import dev.ftb.mods.ftblibrary.snbt.config.ConfigUtil;
import dev.ftb.mods.ftblibrary.snbt.config.IntValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client settings, in {@code local/quest_clusters_visuals/client-config.snbt}. Keys and groups match Certain
 * Questing Additions' {@code local/certain_questing_additions/client-config.snbt}, which is imported on first start.
 */
public final class VisualsConfig {
    public static final SNBTConfig CONFIG = SNBTConfig.create(QuestClustersVisuals.MOD_ID);

    public static final SNBTConfig ANIMATIONS = CONFIG.addGroup("animations");
    public static final BooleanValue QUEST_HOVER = ANIMATIONS.addBoolean("quest_hover", true);
    public static final BooleanValue PANEL_BUTTON_HOVER = ANIMATIONS.addBoolean("panel_button_hover", true);
    public static final BooleanValue PANEL_ANIMATION = ANIMATIONS.addBoolean("panel_animation", true);
    public static final BooleanValue SMOOTH_SCROLLING = ANIMATIONS.addBoolean("smooth_scrolling", true);

    public static final SNBTConfig APPEARANCE = CONFIG.addGroup("appearance");
    public static final BooleanValue SMOOTH_ANIMATIONS = APPEARANCE.addBoolean("smooth_animations", true);
    public static final BooleanValue SHADER_BACKGROUND = APPEARANCE.addBoolean("shader_background", false);
    public static final IntValue BUTTON_HOVER_GLOW_ALPHA = APPEARANCE.addInt("button_hover_glow_alpha", 40, 0, 255);
    public static final IntValue QUEST_HOVER_GLOW_ALPHA = APPEARANCE.addInt("quest_hover_glow_alpha", 100, 0, 255);

    static final String FILE = "client-config.snbt";

    private VisualsConfig() {
    }

    public static Path dir() {
        return ConfigUtil.LOCAL_DIR.resolve(QuestClustersVisuals.MOD_ID);
    }

    public static Path cqaFile() {
        return ConfigUtil.LOCAL_DIR.resolve("certain_questing_additions").resolve(FILE);
    }

    static void load() {
        importFromCqa(cqaFile(), dir().resolve(FILE));
        ConfigUtil.loadDefaulted(CONFIG, dir(), QuestClustersVisuals.MOD_ID, FILE);
    }

    public static void save() {
        CONFIG.save(dir().resolve(FILE));
    }

    /**
     * One-time import: if we have no settings yet and Certain Questing Additions left some, start from those.
     * The key layout is the same, so the file is taken over as is; the original stays where it was.
     *
     * @return true if imported
     */
    static boolean importFromCqa(Path cqa, Path ours) {
        if (Files.exists(ours) || !Files.isRegularFile(cqa)) {
            return false;
        }
        try {
            Files.createDirectories(ours.getParent());
            Files.copy(cqa, ours);
            QuestClustersVisuals.LOGGER.info("Imported Certain Questing Additions settings from {}", cqa);
            return true;
        } catch (IOException e) {
            QuestClustersVisuals.LOGGER.warn("Could not import {}: {}", cqa, e.toString());
            return false;
        }
    }
}
