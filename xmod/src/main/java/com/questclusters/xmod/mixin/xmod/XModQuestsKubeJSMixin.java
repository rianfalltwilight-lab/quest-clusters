package com.questclusters.xmod.mixin.xmod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FTB XMod Compat's KubeJS plugin for FTB Quests (event group FTBQuestsEvents, binding FTBQuests, player data):
 * replaced by this module's plugin with the same names, so it must not register them a second time.
 */
@Pseudo
// 1.21.1 and 1.20.1 names of XMod's FTB Quests KubeJS plugin (Pseudo: the one that is not there is skipped)
@Mixin(targets = {"dev.ftb.mods.ftbxmodcompat.neoforge.ftbquests.kubejs.FTBQuestsKubeJSPlugin",
        "dev.ftb.mods.ftbxmodcompat.ftbquests.kubejs.KubeJSIntegration"}, remap = false)
public abstract class XModQuestsKubeJSMixin {
    @Inject(method = {"init", "registerBindings", "attachPlayerData", "registerEvents"}, at = @At("HEAD"), cancellable = true, require = 4)
    private void quest_clusters_xmod$takeOver(CallbackInfo ci) {
        ci.cancel();
    }
}
