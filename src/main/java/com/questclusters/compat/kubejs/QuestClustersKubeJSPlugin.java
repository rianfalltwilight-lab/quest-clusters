package com.questclusters.compat.kubejs;

import com.questclusters.api.QuestClustersAPI;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

/** Registered through kubejs.plugins.txt; only loaded when KubeJS is installed. */
public class QuestClustersKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("QuestClusters", QuestClustersAPI.class);
    }
}
