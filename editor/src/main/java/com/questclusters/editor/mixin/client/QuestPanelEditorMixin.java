package com.questclusters.editor.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.questclusters.compat.QuestEnhanceCompat;
import com.questclusters.editor.client.gui.DecorativeLinesWidget;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = QuestPanel.class, remap = false)
public abstract class QuestPanelEditorMixin {
    /** After FTB Quests (and core) added theirs: last in the list = drawn after all chapter images. */
    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void quest_clusters_editor$addWidgets(CallbackInfo ci) {
        QuestPanel self = (QuestPanel) (Object) this;
        if (((QuestScreen) self.getGui()).getSelectedChapter().isPresent()) {
            self.getWidgets().add(new DecorativeLinesWidget(self));
        }
    }

    @Inject(method = "alignWidgets", at = @At("TAIL"))
    private void quest_clusters_editor$alignWidgets(CallbackInfo ci) {
        for (Widget w : ((QuestPanel) (Object) this).getWidgets()) {
            if (w instanceof DecorativeLinesWidget d) d.align();
        }
    }

    /**
     * Hidden prerequisite lines: purple in edit mode; for players drawn only if set to show on hover and an end is
     * hovered. No native line is drawn while the details panel is open (they would show through it).
     */
    @WrapOperation(method = "drawOffsetBackground", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/client/gui/quests/QuestPanel;renderConnection(Ldev/ftb/mods/ftbquests/client/gui/quests/QuestButton;Ldev/ftb/mods/ftbquests/client/gui/quests/QuestButton;Lcom/mojang/blaze3d/vertex/PoseStack;FIIIIIFLcom/mojang/blaze3d/vertex/Tesselator;)V"))
    private void quest_clusters_editor$hiddenLines(QuestPanel panel, QuestButton from, QuestButton to, PoseStack ps, float s,
                                                   int r, int g, int b, int a, int a1, float mu, Tesselator t, Operation<Void> op) {
        QuestScreen qs = (QuestScreen) panel.getGui();
        if (qs.isViewingQuest()) {
            return;
        }
        var chapter = qs.getSelectedChapter().orElse(null);
        var hidden = QuestEnhanceCompat.hiddenLine(chapter, from.moveAndDeleteFocus(), to.moveAndDeleteFocus());
        if (hidden.isEmpty()) {
            op.call(panel, from, to, ps, s, r, g, b, a, a1, mu, t);
        } else if (ClientQuestFile.INSTANCE != null && ClientQuestFile.INSTANCE.canEdit()) {
            com.questclusters.editor.client.EditorDebug.hiddenPurple++;
            op.call(panel, from, to, ps, s, 196, 98, 255, a, a1, mu, t);
        } else if (hidden.get().revealOnHover() && (from.isMouseOver() || to.isMouseOver())) {
            com.questclusters.editor.client.EditorDebug.hiddenRevealed++;
            op.call(panel, from, to, ps, s, r, g, b, a, a1, mu, t);
        } else {
            com.questclusters.editor.client.EditorDebug.hiddenSkipped++;
        }
    }
}
