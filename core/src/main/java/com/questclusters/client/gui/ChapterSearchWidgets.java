package com.questclusters.client.gui;

import com.questclusters.client.ClusterViewState;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.TextBox;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.gui.quests.ChapterPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Search box at the top of FTB Quests' chapter list. While a query is typed, the chapter list is replaced by results.
 */
public final class ChapterSearchWidgets {
    private static final Map<ChapterPanel, SearchBox> BOXES = new WeakHashMap<>();

    private ChapterSearchWidgets() {
    }

    /** Called after FTB Quests filled the chapter panel. */
    public static void addWidgets(ChapterPanel panel, QuestScreen screen) {
        SearchBox box = BOXES.computeIfAbsent(panel, p -> new SearchBox(p));
        List<Widget> widgets = panel.getWidgets();
        widgets.add(Math.min(1, widgets.size()), box); // right below the modpack title button
        if (!QuestSearch.active()) {
            return;
        }
        widgets.removeIf(w -> w instanceof ChapterPanel.ChapterButton || w instanceof ChapterPanel.ChapterGroupButton);
        List<QuestSearch.Result> results = QuestSearch.search(QuestSearch.query());
        widgets.add(new HeaderButton(panel, results.isEmpty()
                ? Component.translatable("quest_clusters.search.none")
                : Component.translatable("quest_clusters.search.count", results.size(), results.size() >= QuestSearch.MAX_RESULTS ? "+" : "")));
        for (QuestSearch.Result r : results) {
            widgets.add(new ResultButton(panel, screen, r));
        }
    }

    /** Focus the search box (Ctrl+F) and keep the chapter panel open. */
    public static boolean focus(ChapterPanel panel) {
        SearchBox box = BOXES.get(panel);
        if (box == null) {
            return false;
        }
        panel.setExpanded(true);
        box.setFocused(true);
        return true;
    }

    public static boolean isSearchFocused(ChapterPanel panel) {
        SearchBox box = BOXES.get(panel);
        return box != null && box.isFocused();
    }

    static final class SearchBox extends TextBox {
        private final ChapterPanel chapterPanel;

        SearchBox(ChapterPanel panel) {
            super(panel);
            this.chapterPanel = panel;
            setSize(100, 14);
            ghostText = Component.translatable("quest_clusters.search.hint").getString(); // grey hint while empty
            setText(QuestSearch.query(), false);
        }

        @Override
        public void onTextChanged() {
            // TextBox also calls this when only the cursor moves (including from setText); refresh on real changes only
            if (getText().equals(QuestSearch.query())) {
                return;
            }
            QuestSearch.setQuery(getText());
            chapterPanel.refreshWidgets();
        }
    }

    static final class HeaderButton extends ChapterPanel.ListButton {
        HeaderButton(ChapterPanel panel, Component text) {
            super(panel, text.copy().withStyle(ChatFormatting.GRAY), Icon.empty());
        }

        @Override
        public void onClicked(MouseButton button) {
        }

        @Override
        public void draw(GuiGraphics g, Theme theme, int x, int y, int w, int h) {
            theme.drawString(g, getTitle(), x + 4, y + 3);
        }
    }

    static final class ResultButton extends ChapterPanel.ListButton {
        private final QuestScreen screen;
        private final QuestSearch.Result result;

        ResultButton(ChapterPanel panel, QuestScreen screen, QuestSearch.Result r) {
            super(panel, r.quest() != null ? r.quest().getTitle() : ClusterText.title(r.cluster()),
                    r.quest() != null ? r.quest().getIcon() : ItemIcon.getItemIcon(Items.BOOKSHELF));
            this.screen = screen;
            this.result = r;
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            if (result.quest() != null) {
                screen.open(result.quest(), true); // also expands a collapsed cluster holding it
            } else {
                var c = result.cluster();
                screen.open(result.chapter(), false);
                ClusterViewState.setExpanded(c, true);
                screen.questPanel.scrollTo(c.x(), c.y());
            }
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(getTitle());
            list.add(Component.translatable("quest_clusters.search.in_chapter", result.chapter().getTitle()).withStyle(ChatFormatting.GRAY));
            list.add(Component.translatable("quest_clusters.search.field." + result.field().name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        @Override
        public void draw(GuiGraphics g, Theme theme, int x, int y, int w, int h) {
            if (isMouseOver()) {
                Color4I.WHITE.withAlpha(40).draw(g, x + 1, y, w - 2, h);
            }
            icon.draw(g, x + 2, y + 1, 12, 12);
            theme.drawString(g, getTitle(), x + 16, y + 3);
        }

        @Override
        public int getActualWidth(QuestScreen s) {
            return Math.min(200, s.getTheme().getStringWidth(getTitle()) + 22);
        }
    }

    static boolean isSearchWidget(Widget w) {
        return w instanceof SearchBox || w instanceof HeaderButton || w instanceof ResultButton;
    }
}
