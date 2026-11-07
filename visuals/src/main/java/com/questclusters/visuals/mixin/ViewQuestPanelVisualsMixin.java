package com.questclusters.visuals.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.questclusters.visuals.client.Ease;
import com.questclusters.visuals.client.VisualsConfig;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import dev.ftb.mods.ftbquests.quest.Quest;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Quest details panel slides up 20 px into place when opened (0.3 s). */
@Mixin(value = ViewQuestPanel.class, remap = false)
public abstract class ViewQuestPanelVisualsMixin {
    @Shadow
    private Quest quest;

    @Unique
    private final Ease quest_clusters_visuals$open = new Ease(1F, 0.3F);

    @Inject(method = "setViewedQuest", at = @At("HEAD"))
    private void quest_clusters_visuals$startOpen(Quest newQuest, CallbackInfo ci) {
        if (quest == null && newQuest != null && VisualsConfig.PANEL_ANIMATION.get()) {
            quest_clusters_visuals$open.set(1F, 0F);
        }
    }

    @WrapMethod(method = "draw")
    private void quest_clusters_visuals$slide(GuiGraphics g, Theme theme, int x, int y, int w, int h, Operation<Void> original) {
        float f = quest_clusters_visuals$open.get();
        if (f >= 0.999F) {
            original.call(g, theme, x, y, w, h);
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0, 20F * (1F - f), 0);
        try {
            original.call(g, theme, x, y, w, h);
        } finally {
            g.pose().popPose();
        }
    }
}
