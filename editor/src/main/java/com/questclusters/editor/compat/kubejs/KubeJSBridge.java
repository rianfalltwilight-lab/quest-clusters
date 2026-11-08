package com.questclusters.editor.compat.kubejs;

import net.neoforged.fml.ModList;

/** Calls into KubeJS only when it is installed (no KubeJS class is loaded otherwise). */
public final class KubeJSBridge {
    private static Boolean present;

    private KubeJSBridge() {
    }

    public static boolean present() {
        if (present == null) present = ModList.get() != null && ModList.get().isLoaded("kubejs");
        return present;
    }

    public static void textClicked(String text, long chapterId, double x, double y, double w, double h) {
        if (present()) EditorEvents.postClick(text, chapterId, x, y, w, h);
    }
}
