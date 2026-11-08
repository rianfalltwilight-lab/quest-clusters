package com.questclusters.editor.client.desc;

import com.questclusters.editor.data.DescriptionTable;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** A description table drawn as a grid in the quest details view; right-click (edit mode) to edit or delete it. */
public final class TableWidget extends Widget {
    private final DescriptionTable table;
    private final String payload;
    private final Quest quest;
    private final ViewQuestPanel view;
    private final TableRenderer renderer;

    public TableWidget(Panel panel, DescriptionTable table, String payload, Quest quest, ViewQuestPanel view) {
        super(panel);
        this.table = table;
        this.payload = payload;
        this.quest = quest;
        this.view = view;
        this.renderer = new TableRenderer(table);
        setWidth(panel.width);
        layout(panel.width);
    }

    public DescriptionTable table() {
        return table;
    }

    public int layout(int width) {
        int h = renderer.layout(width);
        setHeight(h);
        return h;
    }

    @Override
    public void draw(GuiGraphics g, Theme theme, int x, int y, int w, int h) {
        renderer.draw(g, x, y, w, null);
    }

    @Override
    public boolean mousePressed(MouseButton button) {
        if (isMouseOver() && button.isRight() && ClientQuestFile.INSTANCE.canEdit()) {
            List<ContextMenuItem> menu = new ArrayList<>();
            menu.add(new ContextMenuItem(Component.translatable("selectServer.edit"), ThemeProperties.EDIT_ICON.get(),
                    b -> TableEditorScreen.open(table.copy(), t -> DescriptionLines.replace(quest, view, payload, t.toLine()))));
            menu.add(new ContextMenuItem(Component.translatable("selectServer.delete"), ThemeProperties.DELETE_ICON.get(),
                    b -> DescriptionLines.replace(quest, view, payload, null)));
            getGui().openContextMenu(menu);
            return true;
        }
        return false;
    }
}
