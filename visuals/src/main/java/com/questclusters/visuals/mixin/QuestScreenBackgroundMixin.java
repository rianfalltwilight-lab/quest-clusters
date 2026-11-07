package com.questclusters.visuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.questclusters.visuals.client.ChapterShaders;
import com.questclusters.visuals.client.VisualsConfig;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** With "shader_background" on, the chapter's shader replaces the theme background behind the quest canvas. */
@Mixin(value = QuestScreen.class, remap = false)
public abstract class QuestScreenBackgroundMixin {
    @Shadow
    Chapter selectedChapter;
    @Shadow
    double scrollWidth;
    @Shadow
    double scrollHeight;
    @Shadow
    @Final
    public QuestPanel questPanel;

    @WrapOperation(method = "drawBackground", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftblibrary/ui/BaseScreen;drawBackground(Lnet/minecraft/client/gui/GuiGraphics;Ldev/ftb/mods/ftblibrary/ui/Theme;IIII)V"))
    private void quest_clusters_visuals$shaderBackground(QuestScreen self, GuiGraphics g, Theme theme, int x, int y, int w, int h,
                                                         Operation<Void> original) {
        if (VisualsConfig.SHADER_BACKGROUND.get()) {
            double cx = questPanel.getScrollX() + questPanel.width / 2D;
            double cy = questPanel.getScrollY() + questPanel.height / 2D;
            float zoom = (float) (((QuestScreen) (Object) this).getQuestButtonSize() * 2D / 3D);
            if (ChapterShaders.draw(g, selectedChapter, x, y, w, h, cx, cy, scrollWidth, scrollHeight, zoom)) {
                return;
            }
        }
        original.call(self, g, theme, x, y, w, h);
    }
}
