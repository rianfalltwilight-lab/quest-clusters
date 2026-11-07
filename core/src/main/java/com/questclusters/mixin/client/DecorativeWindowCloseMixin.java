package com.questclusters.mixin.client;

import com.questclusters.client.gui.DecorativeLineWindow;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** High priority: this TAIL callback runs after other addons' TAIL callbacks, closing the window again. */
@Mixin(value = QuestPanel.class, priority = 1500, remap = false)
public abstract class DecorativeWindowCloseMixin {
    @Inject(method = "drawOffsetBackground", at = @At("TAIL"))
    private void quest_clusters$closeDecorativeWindow(CallbackInfo ci) {
        DecorativeLineWindow.close();
    }
}
