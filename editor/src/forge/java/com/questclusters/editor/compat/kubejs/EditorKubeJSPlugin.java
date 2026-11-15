package com.questclusters.editor.compat.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;

/**
 * Registered through kubejs.plugins.txt; only loaded when KubeJS is installed (KubeJS 6, Minecraft 1.20.1): the
 * canvas text click event. The QuestUtils editing binding is 1.21.1 only (it edits through FTB Quests' history).
 */
public class EditorKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void registerEvents() {
        EditorEvents.GROUP.register();
    }
}
