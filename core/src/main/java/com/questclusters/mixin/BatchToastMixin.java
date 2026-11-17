package com.questclusters.mixin;

import com.questclusters.server.ServerBatch;
import dev.ftb.mods.ftbquests.events.QuestProgressEventData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A batch submit shows the first round's completion toasts only; the rest are summed up in one chat line. */
@Mixin(value = QuestProgressEventData.class, remap = false)
public abstract class BatchToastMixin {
    @Inject(method = "notifyPlayers", at = @At("HEAD"), cancellable = true)
    private void quest_clusters$quietBatch(long id, CallbackInfo ci) {
        if (ServerBatch.quiet()) ci.cancel();
    }
}
