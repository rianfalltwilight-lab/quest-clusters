package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.ClientEditor;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.gui.quests.ChapterPanel;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Quest book sidebar background, stretched over the chapter panel, under the chapter buttons. */
@Mixin(value = ChapterPanel.class, remap = false)
public abstract class ChapterPanelEditorMixin {
    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void quest_clusters_editor$sidebar(GuiGraphics g, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        String bg = ClientEditor.book().sidebarBackground;
        if (!bg.isEmpty()) {
            ClientEditor.icon(bg).draw(g, x, y, w, h);
        }
    }
}
