package com.questclusters.editor.server;

import com.questclusters.editor.data.EditorPatch;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import com.questclusters.platform.History;
import net.minecraft.network.chat.Component;

import java.util.List;

/** An editor change as one undo step (FTB Quests' history on 1.21.1, see platform.History). */
public record EditorChangeEvent(EditorPatch after, EditorPatch before, String action) implements History.Change {
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
    public List<Component> description(BaseQuestFile file) {
        return List.of(Component.translatable("quest_clusters_editor.event." + action));
    }
}
