package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.gui.EditorMenus;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;

/** Right-click on empty canvas: canvas text, guide point, GIF, paste several, transform. */
@Mixin(value = QuestPanel.class, remap = false)
public abstract class QuestPanelMenuMixin {
    @Shadow
    protected double questX;
    @Shadow
    protected double questY;

    @ModifyArg(method = "mousePressed", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/client/gui/quests/QuestScreen;openContextMenu(Ljava/util/List;)Ldev/ftb/mods/ftblibrary/ui/ContextMenu;"))
    private List<ContextMenuItem> quest_clusters_editor$canvasItems(List<ContextMenuItem> menu) {
        List<ContextMenuItem> out = new ArrayList<>(menu);
        EditorMenus.canvasItems(out, (QuestScreen) ((QuestPanel) (Object) this).getGui(), questX, questY);
        return out;
    }
}
