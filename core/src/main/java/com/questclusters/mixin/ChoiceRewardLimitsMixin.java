package com.questclusters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.questclusters.server.ServerLimits;
import dev.ftb.mods.ftbquests.net.ClaimChoiceRewardMessage;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.ChoiceReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Choice rewards hand out the picked reward directly instead of through TeamData#claimReward, so their limit is checked
 * here: a used-up choice gives nothing (it is still marked as claimed).
 */
@Mixin(value = ClaimChoiceRewardMessage.class, remap = false)
public abstract class ChoiceRewardLimitsMixin {
    //? if neoforge {
    @WrapOperation(method = "lambda$handle$0", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/quest/reward/Reward;tryClaim(Lnet/minecraft/server/level/ServerPlayer;Z)Z"))
    private static boolean quest_clusters$limit(Reward picked, ServerPlayer player, boolean notify, Operation<Boolean> original,
                                                @Local(argsOnly = true) ChoiceReward choice, @Local(argsOnly = true) TeamData td) {
        if (ServerLimits.blockClaim(td, player, choice)) return true; // marked as claimed right after, nothing given
        boolean given = original.call(picked, player, notify);
        if (given) ServerLimits.claimed(td, player, choice);
        return given;
    }
    //?} else {
    /*@WrapOperation(method = "handle", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/quest/reward/Reward;claim(Lnet/minecraft/server/level/ServerPlayer;Z)V"))
    private void quest_clusters$limit(Reward picked, ServerPlayer player, boolean notify, Operation<Void> original,
                                      @Local ChoiceReward choice, @Local TeamData td) {
        // the choice itself is claimed (and counted, or refused with a message) by TeamData#claimReward right after
        if (!ServerLimits.limits().exhausted(choice.id, td.getTeamId(), player.getUUID())) original.call(picked, player, notify);
    }
    *///?}
}
