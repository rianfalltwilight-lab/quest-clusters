package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.CanvasEdit;
import com.questclusters.editor.client.gui.EditorMenus;
import com.questclusters.mixin.client.QuestScreenAccessor;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPositionableButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Menu entries for quests, links and chapters (FTB Quests builds all of those through addObjectMenuItems),
 * transform shortcuts, and copying several objects at once. Core's clipboard hook (priority 500) runs first and
 * remembers clusters in the selection, so pasted copies get their clusters back.
 */
@Mixin(value = QuestScreen.class, remap = false)
public abstract class QuestScreenMenuMixin {
    @Inject(method = "addObjectMenuItems", at = @At("TAIL"))
    private void quest_clusters_editor$objectItems(List<ContextMenuItem> menu, Button button, Runnable gui, QuestObjectBase object,
                                                   Movable deletionFocus, CallbackInfo ci) {
        EditorMenus.objectItems(menu, (QuestScreen) (Object) this, object);
    }

    /** Ctrl+Shift+R / Ctrl+Alt+R rotate, Ctrl+Shift+M / Ctrl+Alt+M mirror the selection. */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_editor$transformKeys(Key key, CallbackInfoReturnable<Boolean> cir) {
        if (ClientQuestFile.INSTANCE == null || !ClientQuestFile.INSTANCE.canEdit() || !Screen.hasControlDown()
                || Screen.hasShiftDown() == Screen.hasAltDown()) {
            return;
        }
        int mode;
        if (key.is(GLFW.GLFW_KEY_R)) mode = Screen.hasShiftDown() ? CanvasEdit.ROTATE_CW : CanvasEdit.ROTATE_CCW;
        else if (key.is(GLFW.GLFW_KEY_M)) mode = Screen.hasShiftDown() ? CanvasEdit.MIRROR_H : CanvasEdit.MIRROR_V;
        else return;
        if (CanvasEdit.transform((QuestScreen) (Object) this, mode)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "copyObjectsToClipboard", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_editor$copy(CallbackInfoReturnable<Boolean> cir) {
        QuestScreen self = (QuestScreen) (Object) this;
        List<Movable> sel = ((QuestScreenAccessor) self).quest_clusters$getSelectedObjects();
        if (CanvasEdit.copy(self)) {
            cir.setReturnValue(true);
            return;
        }
        Movable one = sel.size() == 1 ? sel.getFirst() : null;
        if (sel.isEmpty()) {
            for (Widget w : self.questPanel.getWidgets()) {
                if (w instanceof QuestPositionableButton p && w.isMouseOver()) {
                    one = p.moveAndDeleteFocus();
                    break;
                }
            }
        }
        if (one instanceof QuestLink link && CanvasEdit.copyLink(link)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "pasteSelectedQuest", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_editor$paste(boolean withDeps, Chapter chapter, CallbackInfoReturnable<Boolean> cir) {
        if (CanvasEdit.hasMultiClipboard()) {
            var xy = ((QuestScreenAccessor) this).quest_clusters$invokeGetSnappedXY();
            if (CanvasEdit.paste(chapter, xy.getFirst(), xy.getSecond(), withDeps)) {
                cir.setReturnValue(true);
            }
        }
    }
}
