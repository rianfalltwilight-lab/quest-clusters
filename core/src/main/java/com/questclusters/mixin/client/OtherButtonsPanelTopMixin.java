package com.questclusters.mixin.client;

import com.questclusters.client.gui.ScoringButtons;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftbquests.client.gui.quests.OtherButtonsPanelTop;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OtherButtonsPanelTop.class, remap = false)
public abstract class OtherButtonsPanelTopMixin {
    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void quest_clusters$addScoringButtons(CallbackInfo ci) {
        Panel self = (Panel) (Object) this;
        ScoringButtons.addTo(self, (QuestScreen) self.getGui());
    }
}
