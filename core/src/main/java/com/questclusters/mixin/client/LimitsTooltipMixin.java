package com.questclusters.mixin.client;

import com.questclusters.client.ClientLimits;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.gui.quests.RewardButton;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reward tooltips: claims used of the team and player limits, or "used up". */
@Mixin(value = RewardButton.class, remap = false)
public abstract class LimitsTooltipMixin {
    @Shadow
    @Final
    Reward reward;

    @Inject(method = "addMouseOverText", at = @At("TAIL"))
    private void quest_clusters$limits(TooltipList list, CallbackInfo ci) {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        ClientLimits.rewardTooltip(reward.id, lines);
        lines.forEach(list::add);
    }
}
