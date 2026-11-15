package com.questclusters.editor.mixin;

import com.questclusters.editor.perf.AsyncTeamSave;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Before the quest book goes away (server stop), wait for background team progress writes. */
@Mixin(value = ServerQuestFile.class, remap = false)
public abstract class ServerQuestFileFlushMixin {
    @Inject(method = "unload", at = @At("TAIL"))
    private void quest_clusters_editor$flush(CallbackInfo ci) {
        AsyncTeamSave.flush();
    }
}
