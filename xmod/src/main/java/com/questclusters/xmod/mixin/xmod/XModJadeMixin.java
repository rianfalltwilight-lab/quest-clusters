package com.questclusters.xmod.mixin.xmod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FTB XMod Compat's Jade setup for FTB Quests barriers: switched off in favour of this module's Jade plugin. */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbxmodcompat.ftbquests.jade.JadeQuestsSetup", remap = false)
public abstract class XModJadeMixin {
    @Inject(method = "register", at = @At("HEAD"), cancellable = true, require = 1)
    private static void quest_clusters_xmod$takeOver(CallbackInfo ci) {
        ci.cancel();
    }
}
