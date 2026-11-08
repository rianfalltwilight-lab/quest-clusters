package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.desc.Ponder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Description links "quest_enhance:ponder/<item>" open that item's Ponder scenes. */
@Mixin(targets = "dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel$QuestDescriptionField", remap = false)
public abstract class QuestDescriptionFieldMixin {
    @Inject(method = "handleCustomClickEvent", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_editor$ponder(Style style, CallbackInfoReturnable<Boolean> cir) {
        ClickEvent e = style == null ? null : style.getClickEvent();
        if (e != null && e.getAction() == ClickEvent.Action.CHANGE_PAGE && e.getValue().startsWith(Ponder.PREFIX)) {
            Ponder.open(e.getValue().substring(Ponder.PREFIX.length()));
            cir.setReturnValue(true);
        }
    }
}
