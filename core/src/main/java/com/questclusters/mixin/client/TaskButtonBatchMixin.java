package com.questclusters.mixin.client;

import com.questclusters.client.ClientLimits;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.gui.quests.TaskButton;
import dev.ftb.mods.ftbquests.quest.task.Task;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shift-click on a task of a batch-submit quest: submit and finish as many rounds as the items allow. */
@Mixin(value = TaskButton.class, remap = false)
public abstract class TaskButtonBatchMixin {
    @Shadow
    @Final
    Task task;

    @Inject(method = "onClicked", at = @At("HEAD"), cancellable = true)
    private void quest_clusters$batch(MouseButton button, CallbackInfo ci) {
        if (button.isLeft() && net.minecraft.client.gui.screens.Screen.hasShiftDown() && ClientLimits.batchClick(task)) {
            ((TaskButton) (Object) this).playClickSound();
            ci.cancel();
        }
    }

    @Inject(method = "addMouseOverText", at = @At("TAIL"))
    private void quest_clusters$batchTooltip(TooltipList list, CallbackInfo ci) {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        ClientLimits.batchTooltip(task.getQuest(), lines);
        lines.forEach(list::add);
    }
}
