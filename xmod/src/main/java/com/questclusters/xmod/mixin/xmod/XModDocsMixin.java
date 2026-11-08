package com.questclusters.xmod.mixin.xmod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FTB XMod Compat's GuideME and Patchouli targets for FTB Quests' "show docs": provided by this module instead. */
@Pseudo
@Mixin(targets = {"dev.ftb.mods.ftbxmodcompat.neoforge.ftbquests.guideme.GuideMECompat",
        "dev.ftb.mods.ftbxmodcompat.neoforge.ftbquests.patchouli.PatchouliCompat"}, remap = false)
public abstract class XModDocsMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true, require = 1)
    private static void quest_clusters_xmod$takeOver(CallbackInfo ci) {
        ci.cancel();
    }
}
