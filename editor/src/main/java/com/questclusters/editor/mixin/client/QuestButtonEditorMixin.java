package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.ClientEditor;
import com.questclusters.mixin.client.QuestButtonAccessor;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestLinkButton;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Quest box background: over the shape's fill, under its outline and the quest icon. Not on quest links. */
@Mixin(value = QuestButton.class, remap = false)
public abstract class QuestButtonEditorMixin {
    @Inject(method = "draw", at = @At(value = "INVOKE", ordinal = 2,
            target = "Ldev/ftb/mods/ftblibrary/icon/ImageIcon;draw(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"))
    private void quest_clusters_editor$boxBackground(GuiGraphics g, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        if ((Object) this instanceof QuestLinkButton) {
            return;
        }
        long id = ((QuestButtonAccessor) this).quest_clusters$getQuest().id;
        String bg = ClientEditor.book().quest(id).boxBackground();
        if (!bg.isEmpty()) {
            ClientEditor.icon(bg).draw(g, x, y, w, h);
        }
    }
}
