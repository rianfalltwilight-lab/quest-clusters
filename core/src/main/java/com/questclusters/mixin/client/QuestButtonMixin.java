package com.questclusters.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.questclusters.client.ClusterViewState;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.questclusters.client.gui.ClusterEditor;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Quest;
import java.util.Collection;
import java.util.List;

/**
 * Members of a collapsed cluster stay in the panel (so the canvas layout and scroll bounds never jump) but are
 * neither drawn nor clickable. FTB Quests and other addons skip dependency lines of buttons that don't draw.
 * Covers quest link buttons too (they extend QuestButton).
 */
@Mixin(value = QuestButton.class, remap = false)
public abstract class QuestButtonMixin {
    @ModifyReturnValue(method = "isEnabled", at = @At("RETURN"))
    private boolean quest_clusters$hideIfCollapsed(boolean original) {
        return original && !ClusterViewState.isHidden(((QuestButton) (Object) this).moveAndDeleteFocus().getMovableID());
    }

    @ModifyReturnValue(method = "shouldDraw", at = @At("RETURN"))
    private boolean quest_clusters$drawIfNotCollapsed(boolean original) {
        return original && !ClusterViewState.isHidden(((QuestButton) (Object) this).moveAndDeleteFocus().getMovableID());
    }

    @Shadow(remap = false)
    @Final
    protected QuestScreen questScreen;

    /** Context menu with several objects selected. */
    @Inject(method = "addMultiSelectionContextMenuItems", at = @At("TAIL"))
    private void quest_clusters$addSelectionItems(Collection<Quest> selected, List<ContextMenuItem> menu, CallbackInfo ci) {
        QuestScreenAccessor acc = (QuestScreenAccessor) questScreen;
        ClusterEditor.addSelectionMenuItems(menu, questScreen, List.copyOf(acc.quest_clusters$getSelectedObjects()),
                acc.quest_clusters$getSelectedChapter());
    }
}
