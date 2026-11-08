package com.questclusters.xmod.mixin.xmod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FTB XMod Compat's JEI plugin for FTB Quests: its categories, crate subtypes, catalysts and runtime hook are
 * switched off, so only this module's (same ids) are registered. The plugin itself stays, empty.
 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbxmodcompat.ftbquests.jei.FTBQuestsJEIIntegration", remap = false)
public abstract class XModJeiMixin {
    @Inject(method = {"registerItemSubtypes", "registerRecipeCatalysts", "registerCategories", "registerAdvanced",
            "onRuntimeAvailable"}, at = @At("HEAD"), cancellable = true, require = 5)
    private void quest_clusters_xmod$takeOver(CallbackInfo ci) {
        ci.cancel();
    }
}
