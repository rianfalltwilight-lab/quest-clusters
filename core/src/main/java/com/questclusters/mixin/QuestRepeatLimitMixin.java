package com.questclusters.mixin;

import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Completion limit of a repeatable quest: after its last allowed round the quest stays completed instead of resetting.
 * Runs on both sides (the client resets its copy too when it learns a reward was claimed).
 */
@Mixin(value = Quest.class, remap = false)
public abstract class QuestRepeatLimitMixin {
    //? if neoforge {
    @Inject(method = "resetProgressIfRepeatable", at = @At("HEAD"), cancellable = true)
    //?} else {
    /*@Inject(method = "checkRepeatable", at = @At("HEAD"), cancellable = true)
    *///?}
    private void quest_clusters$completionLimit(TeamData td, UUID player, CallbackInfoReturnable<Boolean> cir) {
        Quest q = (Quest) (Object) this;
        var limits = td.getFile().isServerSide() ? com.questclusters.server.ServerLimits.limits()
                : com.questclusters.client.ClientLimits.limits();
        if (q.canBeRepeated() && !limits.mayRepeat(q.id, td.getCompletionCount(q))) {
            cir.setReturnValue(false);
        }
    }
}
