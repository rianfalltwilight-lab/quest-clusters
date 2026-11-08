package com.questclusters.editor.mixin.client;

import dev.ftb.mods.ftblibrary.ui.MultilineTextBox;
import dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MultilineTextEditorScreen.class, remap = false)
public interface MultilineEditorAccessor {
    @Accessor("textBox")
    MultilineTextBox quest_clusters_editor$textBox();

    @Invoker("insertAtEndOfLine")
    void quest_clusters_editor$insertAtEndOfLine(String text);
}
