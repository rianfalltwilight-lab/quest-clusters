package com.questclusters.visuals.mixin;

import com.questclusters.visuals.client.ApngTextures;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "*.apng.png" images play: bind the current frame instead of the file. */
@Mixin(value = ImageIcon.class, remap = false)
public abstract class ImageIconVisualsMixin {
    @Shadow
    @Final
    public ResourceLocation texture;

    @Inject(method = "bindTexture", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_visuals$animated(CallbackInfo ci) {
        if (ApngTextures.bindIfAnimated(texture)) {
            ci.cancel();
        }
    }
}
