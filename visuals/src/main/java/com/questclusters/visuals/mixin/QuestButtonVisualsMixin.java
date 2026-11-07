package com.questclusters.visuals.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.questclusters.visuals.client.Ease;
import com.questclusters.visuals.client.VisualsConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hover animation for quest buttons: the whole original drawing (FTB Quests' and every other addon's hooks inside it)
 * is scaled up to 110 % around the centre, and FTB Quests' hover overlay uses the configured glow strength.
 * Wrapping instead of replacing the method keeps other addons' draw hooks working.
 */
@Mixin(value = QuestButton.class, remap = false)
public abstract class QuestButtonVisualsMixin {
    @Unique
    private final Ease quest_clusters_visuals$hover = new Ease(0F, 0.2F);

    @WrapMethod(method = "draw")
    private void quest_clusters_visuals$scaleOnHover(GuiGraphics g, Theme theme, int x, int y, int w, int h, Operation<Void> original) {
        if (!VisualsConfig.QUEST_HOVER.get()) {
            original.call(g, theme, x, y, w, h);
            return;
        }
        quest_clusters_visuals$hover.set(((Widget) (Object) this).isMouseOver() ? 1F : 0F);
        float f = quest_clusters_visuals$hover.get();
        if (f <= 0.001F) {
            original.call(g, theme, x, y, w, h);
            return;
        }
        float scale = 1F + 0.1F * f;
        g.pose().pushPose();
        g.pose().translate(x + w / 2F, y + h / 2F, 0);
        g.pose().scale(scale, scale, 1F);
        g.pose().translate(-(x + w / 2F), -(y + h / 2F), 0);
        try {
            original.call(g, theme, x, y, w, h);
        } finally {
            g.pose().popPose();
        }
    }

    /** FTB Quests draws its hover highlight as WHITE.withAlpha(100); use the configured strength, faded in. */
    @WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftblibrary/icon/Color4I;withAlpha(I)Ldev/ftb/mods/ftblibrary/icon/Color4I;"))
    private Color4I quest_clusters_visuals$glow(Color4I color, int alpha, Operation<Color4I> original) {
        if (color == Color4I.WHITE && alpha == 100) {
            int glow = VisualsConfig.QUEST_HOVER_GLOW_ALPHA.get();
            float f = VisualsConfig.QUEST_HOVER.get() ? Math.max(0.15F, quest_clusters_visuals$hover.get()) : 1F;
            return original.call(color, Math.round(glow * f));
        }
        return original.call(color, alpha);
    }
}
