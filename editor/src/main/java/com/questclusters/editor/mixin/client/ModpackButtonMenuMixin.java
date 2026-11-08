package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.gui.EditorMenus;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.ChapterPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Right-click on the quest book title (not its pin / add buttons) in edit mode: "Set sidebar background". */
@Mixin(value = ChapterPanel.ModpackButton.class, remap = false)
public abstract class ModpackButtonMenuMixin {
    @Inject(method = "onClicked", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_editor$sidebarMenu(MouseButton button, CallbackInfo ci) {
        Widget self = (Widget) (Object) this;
        if (!button.isRight() || ClientQuestFile.INSTANCE == null || !ClientQuestFile.INSTANCE.canEdit()
                || self.getMouseX() > self.getX() + self.width - 34) {
            return;
        }
        List<ContextMenuItem> menu = new ArrayList<>();
        EditorMenus.sidebarItems(menu, (QuestScreen) self.getGui());
        menu.removeIf(i -> i == ContextMenuItem.SEPARATOR);
        self.getGui().openContextMenu(menu);
        ci.cancel();
    }
}
