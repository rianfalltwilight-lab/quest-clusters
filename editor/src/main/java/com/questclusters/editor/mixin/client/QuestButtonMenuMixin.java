package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.gui.EditorMenus;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Quest;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

/** Menu entries with several objects selected (bulk backgrounds, decorative lines, transform). */
@Mixin(value = QuestButton.class, remap = false)
public abstract class QuestButtonMenuMixin {
    @Shadow
    @Final
    protected QuestScreen questScreen;

    //? if neoforge {
    @Inject(method = "addMultiSelectionContextMenuItems", at = @At("TAIL"))
    private void quest_clusters_editor$selectionItems(Collection<Quest> selected, List<ContextMenuItem> menu, CallbackInfo ci) {
        EditorMenus.selectionItems(menu, questScreen);
    }
    //?} else {
    /*@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "onClicked", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftblibrary/ui/BaseScreen;openContextMenu(Ljava/util/List;)Ldev/ftb/mods/ftblibrary/ui/ContextMenu;"))
    private dev.ftb.mods.ftblibrary.ui.ContextMenu quest_clusters_editor$selectionItems(dev.ftb.mods.ftblibrary.ui.BaseScreen screen,
            List<ContextMenuItem> menu, com.llamalad7.mixinextras.injector.wrapoperation.Operation<dev.ftb.mods.ftblibrary.ui.ContextMenu> op) {
        EditorMenus.selectionItems(menu, questScreen);
        return op.call(screen, menu);
    }
    *///?}
}
