package com.questclusters.mixin.client;

import com.questclusters.client.ClusterViewState;
import com.questclusters.client.gui.ClusterEditor;
import com.questclusters.client.gui.ClusterWidgets;
import com.questclusters.client.gui.ChapterSearchWidgets;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = QuestScreen.class, remap = false)
public abstract class QuestScreenMixin {
    /** Jumping to a quest (pinned quests, #links in descriptions, commands, JEI/EMI lookups) expands its cluster. */
    @Inject(method = "open", at = @At("HEAD"))
    private void quest_clusters$revealTarget(QuestObject object, boolean focus, CallbackInfo ci) {
        ClusterWidgets.reveal(object);
    }

    /** Ctrl+F: focus the chapter search box. */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void quest_clusters$searchShortcut(dev.ftb.mods.ftblibrary.ui.input.Key key,
                                               org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (key.is(org.lwjgl.glfw.GLFW.GLFW_KEY_F) && net.minecraft.client.gui.screens.Screen.hasControlDown()
                && ChapterSearchWidgets.focus(((QuestScreenAccessor) this).quest_clusters$getChapterPanel())) {
            cir.setReturnValue(true);
        }
    }

    /** Context menu of a single object: used by quests, quest links and chapter images alike. */
    @Inject(method = "addObjectMenuItems", at = @At("TAIL"))
    private void quest_clusters$addClusterItems(List<ContextMenuItem> menu, Button button, Runnable gui,
                                                QuestObjectBase object, Movable deletionFocus, CallbackInfo ci) {
        if (deletionFocus != null) {
            ClusterEditor.addObjectMenuItems(menu, (QuestScreen) (Object) this, deletionFocus);
        }
    }

    /** Box selection must not pick up objects hidden inside collapsed clusters. */
    @Inject(method = "selectAllQuestsInBox", at = @At("TAIL"))
    private void quest_clusters$dropHiddenFromSelection(int mouseX, int mouseY, double scrollX, double scrollY, CallbackInfo ci) {
        ((QuestScreenAccessor) this).quest_clusters$getSelectedObjects()
                .removeIf(m -> m != null && ClusterViewState.isHidden(m.getMovableID()));
    }
}
