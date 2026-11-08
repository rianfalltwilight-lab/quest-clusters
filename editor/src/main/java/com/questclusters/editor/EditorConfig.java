package com.questclusters.editor;

import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Client options ({@code config/quest_clusters_editor-client.toml}); same keys as Quest Enhance's client config. */
public final class EditorConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue KILL_TASK_MODELS, OPEN_ADVANCEMENTS, IMPORTED;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        KILL_TASK_MODELS = b.comment("Show the entity model instead of the spawn egg for kill tasks (and quests whose only task is one)")
                .define("render_kill_task_entity_models", true);
        OPEN_ADVANCEMENTS = b.comment("Left-clicking an advancement task opens the advancements screen")
                .define("open_advancement_tasks", true);
        IMPORTED = b.comment("Set once Quest Enhance's client options were copied (internal)").define("imported_quest_enhance", false);
        SPEC = b.build();
    }

    private EditorConfig() {
    }

    /** Copies Quest Enhance's client options once, if its config file exists. */
    static void importQuestEnhance() {
        if (IMPORTED.get()) return;
        Path qe = FMLPaths.CONFIGDIR.get().resolve("quest_enhance-client.toml");
        if (Files.exists(qe)) {
            try {
                String s = Files.readString(qe);
                Boolean kill = bool(s, "render_kill_task_entity_models"), adv = bool(s, "open_advancement_tasks");
                if (kill != null) KILL_TASK_MODELS.set(kill);
                if (adv != null) OPEN_ADVANCEMENTS.set(adv);
                QuestClustersEditor.LOGGER.info("Imported Quest Enhance client options from {}", qe);
            } catch (Exception e) {
                QuestClustersEditor.LOGGER.warn("Could not read {}: {}", qe, e.toString());
            }
        }
        IMPORTED.set(true);
        SPEC.save();
    }

    private static Boolean bool(String toml, String key) {
        Matcher m = Pattern.compile("(?m)^\\s*" + key + "\\s*=\\s*(true|false)").matcher(toml);
        return m.find() ? Boolean.parseBoolean(m.group(1)) : null;
    }
}
