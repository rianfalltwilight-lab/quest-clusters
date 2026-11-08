package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.paste.PasteActions;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ctrl+V with an image on the clipboard (and no text) puts it on the canvas as a chapter image. */
@Mixin(value = QuestScreen.class, remap = false)
public abstract class QuestScreenPasteMixin {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_editor$pasteImage(Key key, CallbackInfoReturnable<Boolean> cir) {
        QuestScreen self = (QuestScreen) (Object) this;
        if (key.is(GLFW.GLFW_KEY_V) && Screen.hasControlDown() && !Screen.hasShiftDown() && ClientQuestFile.INSTANCE != null
                && ClientQuestFile.INSTANCE.canEdit() && !self.isViewingQuest() && PasteActions.maybeImage()
                && PasteActions.pasteOnCanvas(self, true)) {
            cir.setReturnValue(true);
        }
    }
}
