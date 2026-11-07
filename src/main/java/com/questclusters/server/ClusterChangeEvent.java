package com.questclusters.server;

import com.questclusters.data.QuestCluster;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.history.ChangeType;
import dev.ftb.mods.ftbquests.quest.history.QuestBookEditEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A cluster change as an entry in FTB Quests' undo/redo history (the arrows in edit mode, Ctrl+Z / Ctrl+Y),
 * listed in its change log next to its own edits.
 *
 * @param before state before the change, or null if the change created the cluster
 * @param after  state after the change, or null if the change removed the cluster
 */
public record ClusterChangeEvent(QuestCluster before, QuestCluster after) implements QuestBookEditEvent {
    @Override
    public boolean apply(ServerQuestFile file) {
        ServerClusters.setRaw(file, before != null ? before.id() : after.id(), after);
        return true;
    }

    @Override
    public boolean applyUndo(ServerQuestFile file) {
        ServerClusters.setRaw(file, after != null ? after.id() : before.id(), before);
        return true;
    }

    @Override
    public List<Component> description(BaseQuestFile file, ChangeType changeType) {
        String key = before == null ? "create" : after == null ? "dissolve"
                : before.members().equals(after.members()) ? (before.x() != after.x() || before.y() != after.y() ? "move" : "edit")
                : "members";
        QuestCluster shown = after != null ? after : before;
        return List.of(Component.translatable("quest_clusters.event." + key,
                QuestBookEditEvent.sanitizeComponent(com.questclusters.client.gui.ClusterText.text(shown.title()))));
    }
}
