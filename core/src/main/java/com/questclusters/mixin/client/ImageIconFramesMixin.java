package com.questclusters.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.questclusters.client.Textures;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Images with an animated .mcmeta play (chapter images, backgrounds and anything else FTB Library draws as an image):
 * vanilla animates such files only inside texture atlases, so a plain image would show its whole frame strip. The
 * texture coordinates are mapped into the current frame, for any frame layout; "interpolate" blends in the next one.
 */
@Mixin(value = ImageIcon.class, remap = false)
public abstract class ImageIconFramesMixin {
    @Shadow
    @Final
    public ResourceLocation texture;

    @WrapOperation(method = "draw(Lnet/minecraft/client/gui/GuiGraphics;IIII)V", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftblibrary/ui/GuiHelper;drawTexturedRect(Lnet/minecraft/client/gui/GuiGraphics;IIIILdev/ftb/mods/ftblibrary/icon/Color4I;FFFF)V"))
    private void quest_clusters$frame(GuiGraphics g, int x, int y, int w, int h, Color4I color, float u0, float v0, float u1, float v1,
                                      Operation<Void> op) {
        Textures.Frame f = texture == null ? null : Textures.frame(texture, System.currentTimeMillis());
        if (f == null) {
            op.call(g, x, y, w, h, color, u0, v0, u1, v1);
            return;
        }
        op.call(g, x, y, w, h, color, f.u(u0), f.v(v0), f.u(u1), f.v(v1));
        Textures.Frame n = f.next();
        if (n != null && f.blend() > 0F) {
            op.call(g, x, y, w, h, color.withAlpha(Math.round(color.alphaf() * f.blend() * 255F)), n.u(u0), n.v(v0), n.u(u1), n.v(v1));
        }
    }
}
