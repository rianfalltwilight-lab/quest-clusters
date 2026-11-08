package com.questclusters.xmod.mixin.xmod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FTB XMod Compat's REI plugin for FTB Quests: switched off in favour of this module's (same category ids). */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbxmodcompat.ftbquests.rei.FTBQuestsREIIntegration", remap = false)
public abstract class XModReiMixin {
    @Inject(method = {"registerCategories", "registerDisplays", "registerBasicEntryFiltering"},
            at = @At("HEAD"), cancellable = true, require = 3)
    private void quest_clusters_xmod$takeOver(CallbackInfo ci) {
        ci.cancel();
    }
}
