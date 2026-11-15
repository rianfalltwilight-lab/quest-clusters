package com.questclusters.editor.compat;

import com.questclusters.editor.mixin.ChapterImageAccessor;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.ImageClickAction;

/** Chapter image click data (Quest Enhance's canvas elements live there); its form differs by version. */
public final class Images {
    private Images() {
    }

    /** No click action set. */
    public static boolean noClick(ChapterImage img) {
        return img.getClickAction().isNone();
    }

    /** Keeps the action type, replaces its data. */
    public static void setClickData(ChapterImage img, String data) {
        ((ChapterImageAccessor) (Object) img).quest_clusters_editor$setClickAction(img.getClickAction().withData(data));
    }

    /** Replaces the click with plain data (no action), as canvas elements are stored. */
    public static void setElement(ChapterImage img, String data) {
        ((ChapterImageAccessor) (Object) img).quest_clusters_editor$setClickAction(ImageClickAction.NONE.withData(data));
    }
}
