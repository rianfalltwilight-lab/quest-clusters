package com.questclusters.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.questclusters.server.PersonalProgress;
import com.questclusters.server.PersonalSlot;
import com.questclusters.server.PersonalState;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;

/** Personal quests: whose state is loaded, saving without it, and messages about it going only to its owner. */
@Mixin(value = TeamData.class, remap = false)
public abstract class TeamDataPersonalMixin implements PersonalSlot {
    @Unique
    private UUID quest_clusters$loaded;
    @Unique
    private PersonalState quest_clusters$neutral;

    @Override
    public UUID quest_clusters$loaded() {
        return quest_clusters$loaded;
    }

    @Override
    public void quest_clusters$setLoaded(UUID player) {
        quest_clusters$loaded = player;
    }

    @Override
    public PersonalState quest_clusters$neutral() {
        return quest_clusters$neutral;
    }

    @Override
    public void quest_clusters$setNeutral(PersonalState state) {
        quest_clusters$neutral = state;
    }

    @Inject(method = "serializeNBT", at = @At("HEAD"))
    private void quest_clusters$saveWithoutPersonal(CallbackInfoReturnable<?> cir) {
        if (((TeamData) (Object) this).getFile().isServerSide()) PersonalProgress.unload((TeamData) (Object) this);
    }

    @Inject(method = "deserializeNBT", at = @At("HEAD"))
    private void quest_clusters$reloaded(CallbackInfo ci) {
        PersonalProgress.reset((TeamData) (Object) this);
    }

    @ModifyReturnValue(method = "getOnlineMembers", at = @At("RETURN"))
    private Collection<ServerPlayer> quest_clusters$onlyOwner(Collection<ServerPlayer> members) {
        return PersonalProgress.recipients((TeamData) (Object) this, members);
    }

    @Inject(method = "setProgress", at = @At("HEAD"))
    private void quest_clusters$progressIn(Task task, long progress, CallbackInfo ci) {
        PersonalProgress.enterScope(task.id);
    }

    @Inject(method = "setProgress", at = @At("RETURN"))
    private void quest_clusters$progressOut(Task task, long progress, CallbackInfo ci) {
        PersonalProgress.leaveScope(task.id, false);
    }

    @Inject(method = "setStarted", at = @At("HEAD"))
    private void quest_clusters$startedIn(long id, Date time, CallbackInfoReturnable<Boolean> cir) {
        PersonalProgress.enterScope(id);
    }

    @Inject(method = "setStarted", at = @At("RETURN"))
    private void quest_clusters$startedOut(long id, Date time, CallbackInfoReturnable<Boolean> cir) {
        PersonalProgress.leaveScope(id, false);
    }

    @Inject(method = "setCompleted", at = @At("HEAD"))
    private void quest_clusters$completedIn(long id, Date time, CallbackInfoReturnable<Boolean> cir) {
        PersonalProgress.enterScope(id);
    }

    @Inject(method = "setCompleted", at = @At("RETURN"))
    private void quest_clusters$completedOut(long id, Date time, CallbackInfoReturnable<Boolean> cir) {
        PersonalProgress.leaveScope(id, false);
    }

    @Inject(method = "markRewardAsClaimed", at = @At("HEAD"))
    private void quest_clusters$claimIn(UUID player, Reward reward, long time, CallbackInfoReturnable<Boolean> cir) {
        PersonalProgress.enterRewardScope(reward.id);
    }

    @Inject(method = "markRewardAsClaimed", at = @At("RETURN"))
    private void quest_clusters$claimOut(UUID player, Reward reward, long time, CallbackInfoReturnable<Boolean> cir) {
        PersonalProgress.leaveScope(reward.id, true);
    }
}
