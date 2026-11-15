package com.questclusters.compat.kubejs;

import com.questclusters.api.QuestClustersAPI;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;

/** Registered through kubejs.plugins.txt; only loaded when KubeJS is installed (KubeJS 6, Minecraft 1.20.1). */
public class QuestClustersKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("QuestClusters", QuestClustersAPI.class);
    }
}
