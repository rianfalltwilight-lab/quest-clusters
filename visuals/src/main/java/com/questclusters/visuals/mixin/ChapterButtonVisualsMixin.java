package com.questclusters.visuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.questclusters.visuals.client.ButtonHover;
import com.questclusters.visuals.client.Ease;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.gui.quests.ChapterPanel;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Animated hover highlight on the chapter list's ChapterButton. */
@Mixin(value = ChapterPanel.ChapterButton.class, remap = false)
public abstract class ChapterButtonVisualsMixin {
    @Unique
    private final Ease quest_clusters_visuals$hover = new Ease(0F, 0.25F);

    @WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftblibrary/icon/Color4I;withAlpha(I)Ldev/ftb/mods/ftblibrary/icon/Color4I;"))
    private Color4I quest_clusters_visuals$glow(Color4I color, int alpha, Operation<Color4I> original) {
        if (color == Color4I.WHITE && alpha == 40) {
            return original.call(color, ButtonHover.alpha(quest_clusters_visuals$hover));
        }
        return original.call(color, alpha);
    }

    @Inject(method = "draw", at = @At("TAIL"))
    private void quest_clusters_visuals$fade(GuiGraphics g, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        ButtonHover.fadeOut(quest_clusters_visuals$hover, ((Widget) (Object) this).isMouseOver(), g, x + 1, y, w - 2, h);
    }
}
