package com.questclusters.editor.mixin.client;

import com.questclusters.editor.EditorConfig;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.gui.quests.TaskButton;
import dev.ftb.mods.ftbquests.quest.task.AdvancementTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Left-clicking an advancement task opens the advancements screen on that advancement's tab. */
@Mixin(value = TaskButton.class, remap = false)
public abstract class AdvancementTaskClickMixin {
    @Shadow
    @Final
    Task task;

    @Inject(method = "onClicked", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_editor$openAdvancement(MouseButton button, CallbackInfo ci) {
        if (!button.isLeft() || !(task instanceof AdvancementTask) || !EditorConfig.OPEN_ADVANCEMENTS.get()) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var advancements = mc.player.connection.getAdvancements();
        ResourceLocation id = ((com.questclusters.editor.mixin.AdvancementTaskAccessor) task).quest_clusters_editor$advancement();
        var holder = id == null ? null : advancements.get(id);
        if (holder == null) return;
        var node = advancements.getTree().get(holder);
        if (node != null) advancements.setSelectedTab(node.root().holder(), true);
        mc.setScreen(new AdvancementsScreen(advancements));
        ci.cancel();
    }
}
