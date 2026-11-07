package com.questclusters.client.gui;

import com.questclusters.client.ClientClusters;
import com.questclusters.client.ClusterViewState;
import com.questclusters.data.QuestCluster;
import com.questclusters.mixin.client.QuestPanelAccessor;
import com.questclusters.mixin.client.QuestScreenAccessor;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.task.Task;

import java.util.List;

/**
 * Glue between FTB Quests' quest panel and our widgets.
 * <p>
 * Widget order matters: FTB Library draws in list order and dispatches clicks in reverse order.
 * Frames and lines go first (drawn below everything, clicked last); boxes go last (clicked first).
 */
public final class ClusterWidgets {
    private ClusterWidgets() {
    }

    public static QuestScreen screen(QuestPanel panel) {
        return ((QuestPanelAccessor) panel).quest_clusters$getQuestScreen();
    }

    public static Chapter chapter(QuestPanel panel) {
        return ((QuestScreenAccessor) screen(panel)).quest_clusters$getSelectedChapter();
    }

    public static boolean canEdit() {
        return ClientQuestFile.INSTANCE != null && ClientQuestFile.INSTANCE.canEdit();
    }

    public static void addWidgets(QuestPanel panel) {
        Chapter chapter = chapter(panel);
        if (chapter == null) {
            return;
        }
        List<QuestCluster> clusters = ClientClusters.store().byChapter(chapter.id);
        if (clusters.isEmpty()) {
            return;
        }

        List<Widget> widgets = panel.getWidgets();
        int i = 0;
        for (QuestCluster c : clusters) {
            widgets.add(i++, new ClusterFrameWidget(panel, c));
        }
        widgets.add(i, new ClusterLinesWidget(panel));
        for (QuestCluster c : clusters) {
            widgets.add(new ClusterBoxWidget(panel, c));
        }
    }

    public static void alignWidgets(QuestPanel panel) {
        if (chapter(panel) == null) {
            return;
        }
        for (Widget w : panel.getWidgets()) {
            if (w instanceof ClusterBoxWidget b) {
                b.align();
            } else if (w instanceof ClusterFrameWidget f) {
                f.align();
            } else if (w instanceof ClusterLinesWidget l) {
                l.align();
            }
        }
    }

    /** Expands the cluster holding the object FTB Quests is about to show. */
    public static void reveal(QuestObject object) {
        long id;
        if (object instanceof Quest q) {
            id = q.id;
        } else if (object instanceof QuestLink link) {
            id = link.id;
        } else if (object instanceof Task t) {
            id = t.getQuest().id;
        } else {
            return;
        }
        ClientClusters.store().clusterOfMember(id).ifPresent(c -> ClusterViewState.setExpanded(c, true));
    }

    // ------------------------------------------------------------ canvas -> panel coordinates (same maths as QuestPanel#alignWidgets)

    /** Rectangle of an object centred at (cx, cy) with size (cw, ch) in canvas units. */
    static int[] objectRect(QuestPanel panel, double cx, double cy, double cw, double ch) {
        QuestScreen qs = screen(panel);
        double bs = qs.getQuestButtonSize();
        double bp = qs.getQuestButtonSpacing();
        QuestPanelAccessor acc = (QuestPanelAccessor) panel;
        double x = (cx - acc.quest_clusters$getQuestMinX() - cw / 2D) * (bs + bp) + bp / 2D + bp * (cw - 1D) / 2D;
        double y = (cy - acc.quest_clusters$getQuestMinY() - ch / 2D) * (bs + bp) + bp / 2D + bp * (ch - 1D) / 2D;
        return new int[]{(int) Math.round(x), (int) Math.round(y), (int) Math.round(bs * cw), (int) Math.round(bs * ch)};
    }

    /** Rectangle that exactly covers the given movables, in panel coordinates (no padding). */
    static int[] boundsOf(QuestPanel panel, List<Movable> movables) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Movable m : movables) {
            int[] r = objectRect(panel, m.getX(), m.getY(), m.getWidth(), m.getHeight());
            minX = Math.min(minX, r[0]);
            minY = Math.min(minY, r[1]);
            maxX = Math.max(maxX, r[0] + r[2]);
            maxY = Math.max(maxY, r[1] + r[3]);
        }
        return movables.isEmpty() ? null : new int[]{minX, minY, maxX - minX, maxY - minY};
    }

    /** Same rule FTB Quests uses to show quests, quest links and chapter images to players. */
    static boolean isVisibleToPlayer(Movable m) {
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        if (file == null || file.selfTeamData == null) {
            return false;
        }
        if (m instanceof QuestObject qo) {
            return qo.isVisible(file.selfTeamData);
        }
        if (m instanceof dev.ftb.mods.ftbquests.quest.ChapterImage img) {
            return img.shouldShowImage(file.selfTeamData);
        }
        return true;
    }

    static double zoom(QuestPanel panel) {
        return screen(panel).getZoom();
    }

    static boolean blockedByOverlay(QuestPanel panel) {
        QuestScreen qs = screen(panel);
        QuestScreenAccessor acc = (QuestScreenAccessor) qs;
        return acc.quest_clusters$isMovingObjects()
                || qs.viewQuestPanel.isMouseOver()
                || acc.quest_clusters$getChapterPanel().isMouseOver()
                || ((QuestPanelAccessor) panel).quest_clusters$getMouseOverQuest() != null;
    }
}
