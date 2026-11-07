package com.questclusters.mixin.client;

import com.questclusters.client.gui.DecorativeLineWindow;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Low priority: this TAIL callback runs before other addons' TAIL callbacks (Quest Enhance's decorative lines). */
@Mixin(value = QuestPanel.class, priority = 500, remap = false)
public abstract class DecorativeWindowOpenMixin {
    @Inject(method = "drawOffsetBackground", at = @At("TAIL"))
    private void quest_clusters$openDecorativeWindow(CallbackInfo ci) {
        DecorativeLineWindow.open((QuestPanel) (Object) this);
    }
}
