package com.questclusters.mixin;

import com.questclusters.compat.RawIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where the visuals module does not draw entity icons (any server, or a client without it), FTB Library cannot parse
 * "entity:..." icon strings and throws while FTB Quests reads a chapter image, which drops the rest of that image's
 * data on the next save. Keep such strings as inert icons instead.
 */
@Mixin(value = Icon.class, remap = false)
public abstract class IconPreserveMixin {
    @org.spongepowered.asm.mixin.Unique
    private static Boolean quest_clusters$visuals;

    @Inject(method = "getIcon(Ljava/lang/String;)Ldev/ftb/mods/ftblibrary/icon/Icon;", at = @At("HEAD"), cancellable = true)
    private static void quest_clusters$keepUnknown(String id, CallbackInfoReturnable<Icon> cir) {
        if (id != null && id.startsWith("entity:")) {
            if (quest_clusters$visuals == null) {
                // visuals draws entity icons on the client only (it may still be loaded on a server, e.g. from the bundle)
                quest_clusters$visuals = net.neoforged.fml.loading.FMLEnvironment.dist.isClient()
                        && ModList.get() != null && ModList.get().isLoaded("quest_clusters_visuals");
            }
            if (!quest_clusters$visuals) {
                cir.setReturnValue(new RawIcon(id));
            }
        }
    }
}
