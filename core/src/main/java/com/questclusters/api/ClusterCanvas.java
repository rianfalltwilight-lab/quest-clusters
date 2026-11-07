package com.questclusters.api;

import com.questclusters.client.ClientClusters;
import com.questclusters.client.ClusterViewState;
import com.questclusters.client.gui.ClusterBoxWidget;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Movable;
import org.jetbrains.annotations.Nullable;

/** Client-only helpers for modules drawing on the quest canvas. */
public final class ClusterCanvas {
    private ClusterCanvas() {
    }

    /**
     * If the object is hidden inside a collapsed cluster, the cluster's box widget (draw towards it instead);
     * otherwise null.
     */
    @Nullable
    public static Widget standIn(QuestPanel panel, Movable m) {
        long id = m.getMovableID();
        boolean hidden = m instanceof ChapterImage ? ClusterViewState.isHiddenImage(id) : ClusterViewState.isHidden(id);
        if (!hidden) {
            return null;
        }
        var cluster = ClientClusters.store().clusterOfMember(id)
                .or(() -> ClusterViewState.anchorOwner(id).flatMap(ClientClusters.store()::get));
        if (cluster.isEmpty()) {
            return null;
        }
        for (Widget w : panel.getWidgets()) {
            if (w instanceof ClusterBoxWidget b && b.clusterId() == cluster.get().id() && b.shouldDraw()) {
                return b;
            }
        }
        return null;
    }
}
