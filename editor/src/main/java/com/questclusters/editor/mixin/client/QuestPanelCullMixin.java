package com.questclusters.editor.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.questclusters.editor.EditorConfig;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Dependency lines entirely outside the visible quest panel are not drawn. Conservative: a line is kept unless its
 * bounding box, widened by the line thickness, misses the panel completely.
 */
@Mixin(value = QuestPanel.class, remap = false)
public abstract class QuestPanelCullMixin {
    @WrapOperation(method = "drawOffsetBackground", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/client/gui/quests/QuestPanel;renderConnection(Ldev/ftb/mods/ftbquests/client/gui/quests/QuestButton;Ldev/ftb/mods/ftbquests/client/gui/quests/QuestButton;Lcom/mojang/blaze3d/vertex/PoseStack;FIIIIIFLcom/mojang/blaze3d/vertex/Tesselator;)V"))
    private void quest_clusters_editor$cull(QuestPanel panel, QuestButton from, QuestButton to, PoseStack ps, float s,
                                           int r, int g, int b, int a, int a1, float mu, Tesselator t, Operation<Void> op) {
        if (EditorConfig.CULL_LINES.get()) {
            double sx = from.getX() + from.width / 2D, sy = from.getY() + from.height / 2D;
            double ex = to.getX() + to.width / 2D, ey = to.getY() + to.height / 2D;
            double m = s + 4;
            if (Math.max(sx, ex) + m < panel.getX() || Math.min(sx, ex) - m > panel.getX() + panel.width
                    || Math.max(sy, ey) + m < panel.getY() || Math.min(sy, ey) - m > panel.getY() + panel.height) {
                com.questclusters.editor.perf.VisibilityCache.culledLines++;
                return;
            }
        }
        op.call(panel, from, to, ps, s, r, g, b, a, a1, mu, t);
    }
}
