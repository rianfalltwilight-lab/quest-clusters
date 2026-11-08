package com.questclusters.xmod.mixin.xmod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FTB XMod Compat's FTB Quests setup (recipe-viewer helper and item filter adapters): replaced by this module.
 * Pseudo: skipped without XMod; a changed XMod fails here at startup rather than registering everything twice.
 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbxmodcompat.ftbquests.FTBQuestsSetup", remap = false)
public abstract class XModQuestsSetupMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true, require = 1)
    private static void quest_clusters_xmod$takeOver(CallbackInfo ci) {
        ci.cancel();
    }
}
