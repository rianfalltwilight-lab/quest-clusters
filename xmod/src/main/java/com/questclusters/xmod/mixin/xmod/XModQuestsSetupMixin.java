package com.questclusters.xmod.mixin.xmod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FTB XMod Compat's item filter adapters for FTB Quests: replaced by this module. (Its recipe-viewer helper is
 * left alone until this module provides JEI/REI support.)
 * Pseudo: skipped without XMod; a changed XMod fails here at startup rather than registering everything twice.
 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbxmodcompat.ftbquests.filtering.ItemFilteringSetup", remap = false)
public abstract class XModQuestsSetupMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true, require = 1)
    private static void quest_clusters_xmod$takeOver(CallbackInfo ci) {
        ci.cancel();
    }
}
