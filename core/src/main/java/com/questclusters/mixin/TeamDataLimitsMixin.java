package com.questclusters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.questclusters.server.ServerLimits;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reward claim limits. Every way of claiming (button, claim all, auto-claim) ends in this method. A used-up reward is
 * only marked as claimed, so its quest can still finish the round and repeat; a reward that is given out is counted.
 */
@Mixin(value = TeamData.class, remap = false)
public abstract class TeamDataLimitsMixin {
    @Inject(method = "claimReward(Lnet/minecraft/server/level/ServerPlayer;Ldev/ftb/mods/ftbquests/quest/reward/Reward;ZJ)V", at = @At("HEAD"), cancellable = true)
    private void quest_clusters$usedUp(ServerPlayer player, Reward reward, boolean notify, long date, CallbackInfo ci) {
        TeamData td = (TeamData) (Object) this;
        if (!td.getFile().isServerSide() || reward.getQuest() == null || !td.isCompleted(reward.getQuest())
                || td.isRewardClaimed(player.getUUID(), reward)) {
            return; // FTB Quests won't give it anyway
        }
        if (ServerLimits.blockClaim(td, player, reward)) {
            ci.cancel();
            td.markRewardAsClaimed(player.getUUID(), reward, date);
        }
    }

    //? if neoforge {
    @WrapOperation(method = "claimReward(Lnet/minecraft/server/level/ServerPlayer;Ldev/ftb/mods/ftbquests/quest/reward/Reward;ZJ)V", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/quest/reward/Reward;tryClaim(Lnet/minecraft/server/level/ServerPlayer;Z)Z"))
    private boolean quest_clusters$count(Reward reward, ServerPlayer player, boolean notify, Operation<Boolean> original) {
        boolean given = original.call(reward, player, notify);
        if (given) ServerLimits.claimed((TeamData) (Object) this, player, reward);
        return given;
    }
    //?} else {
    /*@WrapOperation(method = "claimReward(Lnet/minecraft/server/level/ServerPlayer;Ldev/ftb/mods/ftbquests/quest/reward/Reward;ZJ)V", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/quest/TeamData;markRewardAsClaimed(Ljava/util/UUID;Ldev/ftb/mods/ftbquests/quest/reward/Reward;J)Z"))
    private boolean quest_clusters$count(TeamData td, java.util.UUID id, Reward reward, long date, Operation<Boolean> original,
                                         @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) ServerPlayer player) {
        boolean marked = original.call(td, id, reward, date);
        if (marked) ServerLimits.claimed(td, player, reward); // given right after
        return marked;
    }
    *///?}
}
