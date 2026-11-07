package com.questclusters.visuals.mixin;

import com.questclusters.visuals.client.EntityIcon;
import com.questclusters.visuals.client.EntityIconSpec;
import dev.ftb.mods.ftblibrary.icon.Icon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** "entity:..." icon strings become entity models (client only). */
@Mixin(value = Icon.class, remap = false)
public abstract class IconVisualsMixin {
    @Inject(method = "getIcon(Ljava/lang/String;)Ldev/ftb/mods/ftblibrary/icon/Icon;", at = @At("HEAD"), cancellable = true)
    private static void quest_clusters_visuals$entityIcon(String id, CallbackInfoReturnable<Icon> cir) {
        if (id != null && id.startsWith(EntityIconSpec.PREFIX)) {
            cir.setReturnValue(EntityIcon.of(id));
        }
    }
}
