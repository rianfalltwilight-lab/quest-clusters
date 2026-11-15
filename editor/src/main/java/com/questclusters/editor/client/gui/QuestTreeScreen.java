package com.questclusters.editor.client.gui;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractButtonListScreen;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tree view of the quest layout (as in Misanthropy's FTB Quest Additions; its details are not documented, so this
 * is our design): quests as a dependency tree. A quest sits under its first prerequisite in the same chapter;
 * quests without one are roots, in reading order (top to bottom, left to right). Further prerequisites are named on
 * the row. Players see only what they could see in the book. Click a row to open the quest, click its arrow to fold
 * it; the search box filters rows.
 */
public final class QuestTreeScreen extends AbstractButtonListScreen {
    private static boolean allChapters;
    private static final Set<Long> COLLAPSED = new HashSet<>();

    private final QuestScreen questScreen;
    private final SimpleTextButton scopeButton;

    /** One row: a chapter heading or a quest at some depth. */
    public record Row(Chapter chapter, Quest quest, int depth, boolean hasChildren, List<Quest> otherParents) {
    }

    public QuestTreeScreen(QuestScreen questScreen) {
        this.questScreen = questScreen;
        setTitle(Component.translatable("quest_clusters_editor.tree"));
        setHasSearchBox(true);
        setBorder(1, 1, 1);
        scopeButton = new SimpleTextButton(topPanel, scopeTitle(), ItemIcon.getItemIcon(Items.BOOKSHELF)) {
            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                allChapters = !allChapters;
                setTitle(scopeTitle());
                QuestTreeScreen.this.refreshWidgets();
            }
        };
    }

    private static Component scopeTitle() {
        return Component.translatable(allChapters ? "quest_clusters_editor.tree.all" : "quest_clusters_editor.tree.chapter");
    }

    /** The rows for the given scope (visible to the player unless editing). */
    public static List<Row> rows(Chapter current, boolean all) {
        List<Row> out = new ArrayList<>();
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        if (file == null || file.selfTeamData == null) return out;
        if (!all) {
            if (current != null) chapterRows(current, 0, out);
            return out;
        }
        file.forAllChapters(ch -> {
            if (visible(ch)) {
                out.add(new Row(ch, null, 0, !ch.getQuests().isEmpty(), List.of()));
                if (!COLLAPSED.contains(ch.id)) chapterRows(ch, 1, out);
            }
        });
        return out;
    }

    private static boolean visible(QuestObject o) {
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        return file.canEdit() || o.isVisible(file.selfTeamData);
    }

    private static boolean visible(Chapter c) {
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        return file.canEdit() || c.isVisible(file.selfTeamData);
    }

    private static void chapterRows(Chapter ch, int baseDepth, List<Row> out) {
        List<Quest> quests = ch.getQuests().stream().filter(QuestTreeScreen::visible)
                .sorted(Comparator.comparingDouble(Quest::getY).thenComparingDouble(Quest::getX)).toList();
        Set<Quest> inChapter = new HashSet<>(quests);
        Map<Quest, List<Quest>> children = new LinkedHashMap<>();
        Map<Quest, Quest> parent = new LinkedHashMap<>();
        for (Quest q : quests) {
            for (QuestObject dep : q.streamDependencies().toList()) {
                if (dep instanceof Quest p && inChapter.contains(p) && p != q) {
                    parent.put(q, p);
                    children.computeIfAbsent(p, k -> new ArrayList<>()).add(q);
                    break;
                }
            }
        }
        Set<Quest> placed = new HashSet<>();
        for (Quest q : quests) {
            if (!parent.containsKey(q)) walk(q, baseDepth, children, placed, out);
        }
        for (Quest q : quests) {
            if (!placed.contains(q)) walk(q, baseDepth, children, placed, out); // dependency loops
        }
    }

    private static void walk(Quest q, int depth, Map<Quest, List<Quest>> children, Set<Quest> placed, List<Row> out) {
        if (!placed.add(q)) return;
        List<Quest> kids = children.getOrDefault(q, List.of());
        List<Quest> others = q.streamDependencies().filter(d -> d instanceof Quest).map(d -> (Quest) d)
                .filter(QuestTreeScreen::visible).skip(1).toList();
        out.add(new Row(q.getChapter(), q, depth, !kids.isEmpty(), others));
        if (COLLAPSED.contains(q.id)) {
            kids.forEach(k -> markPlaced(k, children, placed));
            return;
        }
        for (Quest k : kids) walk(k, depth + 1, children, placed, out);
    }

    private static void markPlaced(Quest q, Map<Quest, List<Quest>> children, Set<Quest> placed) {
        if (placed.add(q)) children.getOrDefault(q, List.of()).forEach(k -> markPlaced(k, children, placed));
    }

    @Override
    public void addButtons(Panel panel) {
        Chapter current = ((com.questclusters.mixin.client.QuestScreenAccessor) questScreen).quest_clusters$getSelectedChapter();
        for (Row r : rows(current, allChapters)) panel.add(new RowButton(panel, r));
    }

    @Override
    protected int getTopPanelHeight() {
        return 40;
    }

    @Override
    protected Panel createTopPanel() {
        return new TreeTopPanel();
    }

    @Override
    public boolean onInit() {
        setWidth(Math.min(320, getGui().getScreen().getGuiScaledWidth() - 20));
        setHeight(getGui().getScreen().getGuiScaledHeight() * 4 / 5);
        return true;
    }

    @Override
    public boolean onClosedByKey(Key key) {
        if (super.onClosedByKey(key)) {
            doCancel();
            return true;
        }
        return false;
    }

    @Override
    protected void doCancel() {
        questScreen.openGui();
    }

    @Override
    protected void doAccept() {
        questScreen.openGui();
    }

    private class TreeTopPanel extends ButtonListTopPanel {
        @Override
        public void addWidgets() {
            super.addWidgets();
            add(scopeButton);
        }

        @Override
        public void alignWidgets() {
            super.alignWidgets();
            scopeButton.setPosAndSize(width - scopeButton.width - 2, 1, scopeButton.width, 14);
        }
    }

    private class RowButton extends SimpleTextButton {
        private final Row row;

        RowButton(Panel panel, Row row) {
            super(panel, label(row), row.quest != null ? row.quest.getIcon() : row.chapter.getIcon());
            this.row = row;
            setHeight(16);
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            int arrowEnd = getX() + 20 + row.depth * 10 + 10;
            if (row.hasChildren && getMouseX() < arrowEnd) {
                long id = row.quest != null ? row.quest.id : row.chapter.id;
                if (!COLLAPSED.remove(id)) COLLAPSED.add(id);
                QuestTreeScreen.this.refreshWidgets();
                return;
            }
            questScreen.openGui();
            if (row.quest != null) questScreen.open(row.quest, false);
            else questScreen.selectChapter(row.chapter);
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            if (row.quest == null) return;
            list.add(row.quest.getTitle());
            if (!row.otherParents.isEmpty()) {
                MutableComponent also = Component.translatable("quest_clusters_editor.tree.also");
                for (int i = 0; i < row.otherParents.size(); i++) {
                    also.append(i == 0 ? " " : ", ").append(row.otherParents.get(i).getTitle());
                }
                list.add(also.withStyle(ChatFormatting.GRAY));
            }
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            if (isMouseOver) Color4I.WHITE.withAlpha(30).draw(graphics, x, y, w, h);
            if (row.quest == null) Color4I.WHITE.withAlpha(12).draw(graphics, x, y, w, h);
        }
    }

    private static Component label(Row r) {
        String indent = "  ".repeat(r.depth);
        String arrow = !r.hasChildren ? "   " : COLLAPSED.contains(r.quest != null ? r.quest.id : r.chapter.id) ? "▸ " : "▾ ";
        if (r.quest == null) {
            return Component.literal(indent + arrow).append(r.chapter.getTitle().copy().withStyle(ChatFormatting.BOLD));
        }
        TeamData td = ClientQuestFile.INSTANCE.selfTeamData;
        ChatFormatting state = td.isCompleted(r.quest) ? ChatFormatting.GREEN : td.canStartTasks(r.quest) ? ChatFormatting.WHITE : ChatFormatting.GRAY;
        MutableComponent c = Component.literal(indent + arrow).append(r.quest.getTitle().copy().withStyle(state));
        if (td.isCompleted(r.quest)) c.append(Component.literal(" ✔").withStyle(ChatFormatting.GREEN));
        if (!r.otherParents.isEmpty()) c.append(Component.literal(" +" + r.otherParents.size()).withStyle(ChatFormatting.DARK_GRAY));
        return c;
    }
}
