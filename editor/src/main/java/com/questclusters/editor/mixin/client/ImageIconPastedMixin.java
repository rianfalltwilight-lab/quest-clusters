package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.paste.PastedImages;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pasted images show at once (and after a paste on another session), without reloading resources. */
@Mixin(value = ImageIcon.class, remap = false)
public abstract class ImageIconPastedMixin {
    @Shadow
    @Final
    public net.minecraft.resources.ResourceLocation texture;

    @Inject(method = "bindTexture", at = @At("HEAD"))
    private void quest_clusters_editor$ensure(CallbackInfo ci) {
        PastedImages.ensureRegistered(texture);
    }
}
