package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.desc.LineRender;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Description line markup: decorations around the text of registered lines (others are untouched). */
@Mixin(value = TextField.class, remap = false)
public abstract class DescriptionLineDrawMixin {
    @Unique
    private boolean quest_clusters_editor$pushed;

    @Inject(method = "draw", at = @At("HEAD"))
    private void quest_clusters_editor$before(GuiGraphics g, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        quest_clusters_editor$pushed = LineRender.before((TextField) (Object) this, g, theme, x, y, w, h);
    }

    @Inject(method = "draw", at = @At("TAIL"))
    private void quest_clusters_editor$after(GuiGraphics g, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        LineRender.after((TextField) (Object) this, g, theme, x, y, w, h, quest_clusters_editor$pushed);
        quest_clusters_editor$pushed = false;
    }
}
