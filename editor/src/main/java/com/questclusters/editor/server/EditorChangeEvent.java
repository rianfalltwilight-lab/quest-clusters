package com.questclusters.editor.server;

import com.questclusters.editor.data.EditorPatch;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.history.ChangeType;
import dev.ftb.mods.ftbquests.quest.history.QuestBookEditEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** An editor change as one step in FTB Quests' undo/redo history. */
public record EditorChangeEvent(EditorPatch after, EditorPatch before, String action) implements QuestBookEditEvent {
    @Override
    public boolean apply(ServerQuestFile file) {
        ServerEditor.applyRaw(file, after);
        return true;
    }

    @Override
    public boolean applyUndo(ServerQuestFile file) {
        ServerEditor.applyRaw(file, before);
        return true;
    }

    @Override
    public List<Component> description(BaseQuestFile file, ChangeType changeType) {
        return List.of(Component.translatable("quest_clusters_editor.event." + action));
    }
}
