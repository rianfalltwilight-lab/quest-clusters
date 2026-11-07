package com.questclusters.mixin.client;

import com.questclusters.client.gui.ClusterWidgets;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** TAIL only: FTB Quests builds and positions its widgets first, then we add ours. */
@Mixin(value = QuestPanel.class, remap = false)
public abstract class QuestPanelMixin {
    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void quest_clusters$addWidgets(CallbackInfo ci) {
        ClusterWidgets.addWidgets((QuestPanel) (Object) this);
    }

    @Inject(method = "alignWidgets", at = @At("TAIL"))
    private void quest_clusters$alignWidgets(CallbackInfo ci) {
        ClusterWidgets.alignWidgets((QuestPanel) (Object) this);
    }
}
