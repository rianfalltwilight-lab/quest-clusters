package com.questclusters.editor.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

/** Registered through kubejs.plugins.txt; only loaded when KubeJS is installed. */
public class EditorKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("QuestUtils", QuestUtils.class);
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(EditorEvents.GROUP);
    }
}
