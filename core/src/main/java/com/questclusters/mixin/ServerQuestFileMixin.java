package com.questclusters.mixin;

import com.questclusters.net.ClusterNetwork;
import com.questclusters.server.ServerClusters;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbteams.api.event.PlayerLoggedInAfterTeamEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Only HEAD/TAIL/RETURN injections, so other FTB Quests addons can hook the same methods without conflicts.
 */
@Mixin(value = ServerQuestFile.class, remap = false)
public abstract class ServerQuestFileMixin {
    @Inject(method = "load(ZZ)V", at = @At("TAIL"))
    private void quest_clusters$afterLoad(boolean quests, boolean progression, CallbackInfo ci) {
        if (quests) {
            ServerClusters.onQuestsLoaded((ServerQuestFile) (Object) this);
        }
    }

    @Inject(method = "saveNow", at = @At("HEAD"))
    private void quest_clusters$beforeSave(CallbackInfo ci) {
        ServerClusters.onQuestsSaving((ServerQuestFile) (Object) this);
    }

    @Inject(method = "markDirty", at = @At("TAIL"))
    private void quest_clusters$onDirty(CallbackInfo ci) {
        ServerClusters.onQuestFileDirty();
    }

    @Inject(method = "playerLoggedIn", at = @At("TAIL"))
    private void quest_clusters$onLogin(PlayerLoggedInAfterTeamEvent event, CallbackInfo ci) {
        ClusterNetwork.sendFullSync(event.getPlayer());
        com.questclusters.server.ServerScoring.sendTo(event.getPlayer());
    }

    @Inject(method = "unload", at = @At("TAIL"))
    private void quest_clusters$afterUnload(CallbackInfo ci) {
        ServerClusters.onUnload();
    }
}
