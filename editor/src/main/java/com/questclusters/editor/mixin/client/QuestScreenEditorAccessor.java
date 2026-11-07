package com.questclusters.editor.mixin.client;

import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = QuestScreen.class, remap = false)
public interface QuestScreenEditorAccessor {
    @Accessor("prevMouseX")
    void quest_clusters_editor$setPrevMouseX(int v);

    @Accessor("prevMouseY")
    void quest_clusters_editor$setPrevMouseY(int v);

    @Invoker("selectAllQuestsInBox")
    void quest_clusters_editor$selectAllInBox(int mouseX, int mouseY, double scrollX, double scrollY);
}
