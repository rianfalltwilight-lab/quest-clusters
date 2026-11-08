package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.paste.PasteActions;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ctrl+V with an image on the clipboard in the description editor adds it as an image line. */
@Mixin(value = MultilineTextEditorScreen.class, remap = false)
public abstract class DescriptionPasteMixin {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_editor$pasteImage(Key key, CallbackInfoReturnable<Boolean> cir) {
        if (key.is(GLFW.GLFW_KEY_V) && Screen.hasControlDown() && !Screen.hasShiftDown() && PasteActions.maybeImage()
                && PasteActions.pasteInDescription((MultilineTextEditorScreen) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
