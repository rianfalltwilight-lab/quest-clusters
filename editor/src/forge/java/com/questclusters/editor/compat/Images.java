package com.questclusters.editor.compat;

import com.questclusters.editor.mixin.ChapterImageAccessor;
import dev.ftb.mods.ftbquests.quest.ChapterImage;

/** Chapter image click data (Quest Enhance's canvas elements live there); its form differs by version. */
public final class Images {
    private Images() {
    }

    /** No click set. */
    public static boolean noClick(ChapterImage img) {
        return img.getClick().isEmpty();
    }

    /** 1.20.1 clicks are one string: replaces it. */
    public static void setClickData(ChapterImage img, String data) {
        ((ChapterImageAccessor) (Object) img).quest_clusters_editor$setClick(data);
    }

    /** Replaces the click with the element's data, as canvas elements are stored. */
    public static void setElement(ChapterImage img, String data) {
        setClickData(img, data);
    }
}
