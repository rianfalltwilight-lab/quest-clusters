package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.ClientEditor;
import com.questclusters.mixin.client.QuestScreenAccessor;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Movable;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(value = QuestScreen.class, remap = false)
public abstract class QuestScreenEditorMixin {
    @Unique
    private Set<Movable> quest_clusters_editor$selectedBefore;

    /** Chapter background: stretched over the whole quest screen, fixed (does not scroll with the canvas). */
    @Inject(method = "drawBackground", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Ldev/ftb/mods/ftblibrary/ui/BaseScreen;drawBackground(Lnet/minecraft/client/gui/GuiGraphics;Ldev/ftb/mods/ftblibrary/ui/Theme;IIII)V"))
    private void quest_clusters_editor$chapterBackground(GuiGraphics g, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        var chapter = ((QuestScreenAccessor) this).quest_clusters$getSelectedChapter();
        if (chapter != null) {
            String bg = ClientEditor.book().chapter(chapter.id).background;
            if (!bg.isEmpty()) {
                ClientEditor.icon(bg).draw(g, x, y, w, h);
            }
        }
    }

    @Inject(method = "selectAllQuestsInBox", at = @At("HEAD"))
    private void quest_clusters_editor$boxSelectStart(int mouseX, int mouseY, double sx, double sy, CallbackInfo ci) {
        quest_clusters_editor$selectedBefore = new HashSet<>(((QuestScreenAccessor) this).quest_clusters$getSelectedObjects());
    }

    /** Images flagged "exclude from drag-box selection" can still be clicked, just not boxed. */
    @Inject(method = "selectAllQuestsInBox", at = @At("TAIL"))
    private void quest_clusters_editor$boxSelectEnd(int mouseX, int mouseY, double sx, double sy, CallbackInfo ci) {
        Set<Movable> before = quest_clusters_editor$selectedBefore;
        quest_clusters_editor$selectedBefore = null;
        var excluded = ClientEditor.book().excludedImages;
        if (excluded.isEmpty()) {
            return;
        }
        // as if the box never touched them: keep their selection state from before
        var selected = ((QuestScreenAccessor) this).quest_clusters$getSelectedObjects();
        var chapter = ((QuestScreenAccessor) this).quest_clusters$getSelectedChapter();
        if (chapter == null) {
            return;
        }
        for (ChapterImage img : chapter.getImages()) {
            if (!excluded.contains(img.id)) continue;
            boolean was = before != null && before.contains(img);
            if (was && !selected.contains(img)) selected.add(img);
            else if (!was) selected.remove(img);
        }
    }
}
