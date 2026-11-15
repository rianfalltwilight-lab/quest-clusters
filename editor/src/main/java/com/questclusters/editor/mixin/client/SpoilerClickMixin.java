package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.desc.LineRender;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A covered spoiler line is revealed by a left click (its links only work once it is shown). */
@Mixin(targets = "dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel$QuestDescriptionField", remap = false)
public abstract class SpoilerClickMixin {
    @Inject(method = "mousePressed", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_editor$reveal(MouseButton button, CallbackInfoReturnable<Boolean> cir) {
        Widget self = (Widget) (Object) this;
        var info = LineRender.info(self);
        if (info != null && info.markup().spoiler() && !LineRender.revealed(info) && button.isLeft() && self.isMouseOver()) {
            LineRender.reveal(info);
            self.playClickSound();
            cir.setReturnValue(true);
        }
    }
}
