package com.questclusters.mixin.forge;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.questclusters.client.gui.ChapterSearchWidgets;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.gui.quests.ChapterPanel;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/**
 * 1.20.1: FTB Quests' chapter panel lays out only its own list buttons (it casts every child to one). The search box
 * is taken out while it does, then put back in its row with everything below it moved down.
 */
@Mixin(value = ChapterPanel.class, remap = false)
public abstract class ChapterPanelSearchLayoutMixin {
    @WrapMethod(method = "alignWidgets")
    private void quest_clusters$layoutWithSearch(Operation<Void> original) {
        ChapterPanel panel = (ChapterPanel) (Object) this;
        Widget box = ChapterSearchWidgets.searchBox(panel);
        List<Widget> widgets = panel.getWidgets();
        int index = box == null ? -1 : widgets.indexOf(box);
        if (index < 0) {
            original.call();
            return;
        }
        widgets.remove(index);
        try {
            original.call();
        } finally {
            widgets.add(index, box);
        }
        int y = index == 0 ? 0 : widgets.get(index - 1).posY + widgets.get(index - 1).height;
        int x = widgets.size() > 1 ? widgets.get(index == 0 ? 1 : 0).posX : 0;
        box.setPosAndSize(x + 2, y + 1, panel.width - x - 4, 14);
        for (int i = index + 1; i < widgets.size(); i++) {
            widgets.get(i).setY(widgets.get(i).posY + 16);
        }
    }
}
