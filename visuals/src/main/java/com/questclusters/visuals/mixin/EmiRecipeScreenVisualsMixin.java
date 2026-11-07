package com.questclusters.visuals.mixin;

import dev.ftb.mods.ftblibrary.ui.ScreenWrapper;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * EMI: a recipe screen opened from the quest book goes back to the quest book when closed. Applies only when EMI is
 * installed (pseudo target).
 */
@Pseudo
@Mixin(targets = "dev.emi.emi.screen.RecipeScreen", remap = false)
public abstract class EmiRecipeScreenVisualsMixin {
    @Unique
    private Screen quest_clusters_visuals$questBook;

    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void quest_clusters_visuals$remember(CallbackInfo ci) {
        Screen current = Minecraft.getInstance().screen;
        if (current instanceof ScreenWrapper w && w.getGui() instanceof QuestScreen) {
            quest_clusters_visuals$questBook = current;
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true, require = 0)
    private void quest_clusters_visuals$back(CallbackInfo ci) {
        if (quest_clusters_visuals$questBook != null) {
            Minecraft.getInstance().setScreen(quest_clusters_visuals$questBook);
            ci.cancel();
        }
    }
}
