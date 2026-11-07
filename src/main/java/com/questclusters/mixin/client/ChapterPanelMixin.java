package com.questclusters.mixin.client;

import com.questclusters.client.gui.ChapterSearchWidgets;
import dev.ftb.mods.ftbquests.client.gui.quests.ChapterPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChapterPanel.class, remap = false)
public abstract class ChapterPanelMixin {
    @Shadow
    @Final
    QuestScreen questScreen;

    /** Don't slide the chapter panel away while the search box has keyboard focus. */
    @Inject(method = "setExpanded", at = @At("HEAD"), cancellable = true)
    private void quest_clusters$stayOpenWhileSearching(boolean expanded, CallbackInfo ci) {
        if (!expanded && ChapterSearchWidgets.isSearchFocused((ChapterPanel) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void quest_clusters$addSearch(CallbackInfo ci) {
        ChapterSearchWidgets.addWidgets((ChapterPanel) (Object) this, questScreen);
    }
}
