package com.questclusters.editor;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common options ({@code config/quest_clusters_editor-common.toml}). */
public final class EditorServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ASYNC_TEAM_SAVE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ASYNC_TEAM_SAVE = b.comment("Write FTB Quests team progress files on a background thread (the data is still captured on the server thread; pending writes finish before the server stops)")
                .define("async_team_save", true);
        SPEC = b.build();
    }

    private EditorServerConfig() {
    }
}
