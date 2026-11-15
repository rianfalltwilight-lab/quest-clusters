package com.questclusters.mixin;

import com.questclusters.server.PersonalProgress;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.util.FTBQuestsInventoryListener;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Item detection finds the team without looking up the player's team data: put the player's personal state in here. */
@Mixin(value = FTBQuestsInventoryListener.class, remap = false)
public abstract class InventoryDetectMixin {
    @Inject(method = "detect", at = @At("HEAD"))
    private static void quest_clusters$personal(ServerPlayer player, ItemStack stack, long sourceTask, CallbackInfo ci) {
        if (!PersonalProgress.active() || ServerQuestFile.INSTANCE == null) return;
        FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                .ifPresent(team -> PersonalProgress.enter(ServerQuestFile.INSTANCE.getNullableTeamData(team.getId()), player));
    }
}
