package com.questclusters.mixin;

import com.questclusters.server.PersonalProgress;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FTB Quests looks up a player's team data before acting for them: that is when their personal state goes in. */
@Mixin(value = BaseQuestFile.class, remap = false)
public abstract class QuestFileLookupMixin {
    @Inject(method = "getOrCreateTeamData(Lnet/minecraft/world/entity/Entity;)Ldev/ftb/mods/ftbquests/quest/TeamData;", at = @At("RETURN"))
    private void quest_clusters$personal(Entity entity, CallbackInfoReturnable<TeamData> cir) {
        if (entity instanceof Player p) PersonalProgress.enter(cir.getReturnValue(), p);
    }

    //? if neoforge {
    @Inject(method = "getTeamData(Lnet/minecraft/world/entity/player/Player;)Ljava/util/Optional;", at = @At("RETURN"))
    private void quest_clusters$personalOptional(Player player, CallbackInfoReturnable<java.util.Optional<TeamData>> cir) {
        cir.getReturnValue().ifPresent(td -> PersonalProgress.enter(td, player));
    }
    //?}
}
