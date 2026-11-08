package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.desc.GifIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

/** Icon strings naming a .gif resource play it: description images, chapter images, quest icons alike. */
@Mixin(value = Icon.class, remap = false)
public abstract class IconGifMixin {
    @Inject(method = "getIcon(Ljava/lang/String;)Ldev/ftb/mods/ftblibrary/icon/Icon;", at = @At("HEAD"), cancellable = true)
    private static void quest_clusters_editor$gif(String id, CallbackInfoReturnable<Icon> cir) {
        if (id != null && id.toLowerCase(Locale.ROOT).endsWith(".gif") && !id.contains(" ")) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) cir.setReturnValue(new GifIcon(rl));
        }
    }
}
