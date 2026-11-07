package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.gui.CanvasRender;
import com.questclusters.editor.data.CanvasElement;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.gui.quests.ChapterImageButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Special canvas elements (text, guide points, ...) are drawn by the editor instead of as an image. */
@Mixin(value = ChapterImageButton.class, remap = false)
public abstract class ChapterImageButtonEditorMixin {
    @Shadow
    @Final
    private ChapterImage chapterImage;

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_editor$drawSpecial(GuiGraphics g, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        CanvasElement el = CanvasElement.parse(chapterImage.getClickAction().actionData());
        if (el == null) {
            return;
        }
        boolean selected = ((com.questclusters.mixin.client.QuestScreenAccessor) ((ChapterImageButton) (Object) this).getGui())
                .quest_clusters$getSelectedObjects().contains(chapterImage);
        if (CanvasRender.draw(g, theme, chapterImage, el, selected, x, y, w, h)) {
            ci.cancel();
        }
    }
}
