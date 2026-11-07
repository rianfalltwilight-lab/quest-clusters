package com.questclusters.client.gui;

import com.questclusters.client.ClientClusters;
import com.questclusters.client.ClusterViewState;
import com.questclusters.data.QuestCluster;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPositionableButton;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Movable;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quest Enhance draws its decorative lines at the end of the quest panel's background pass, looking up the canvas
 * buttons through {@code getWidgets()}. For exactly that moment (opened just before its hook, closed just after),
 * the panel hands out a list in which objects hidden inside collapsed clusters are replaced by stand-ins sitting on
 * the cluster box. Its lines then end at the box instead of at invisible quests. Nothing else sees the swap.
 */
public final class DecorativeLineWindow {
    private static QuestPanel active;
    /** For the dev self-test: how many lists were swapped. */
    public static int swaps;

    private DecorativeLineWindow() {
    }

    public static void open(QuestPanel panel) {
        active = panel;
    }

    public static void close() {
        active = null;
    }

    public static List<Widget> filter(Panel panel, List<Widget> widgets) {
        if (panel != active) {
            return widgets;
        }
        Map<Long, ClusterBoxWidget> boxes = new HashMap<>();
        for (Widget w : widgets) {
            if (w instanceof ClusterBoxWidget b && b.shouldDraw()) {
                boxes.put(b.clusterId(), b);
            }
        }
        if (boxes.isEmpty()) {
            return widgets;
        }
        List<Widget> out = new ArrayList<>(widgets.size());
        boolean changed = false;
        for (Widget w : widgets) {
            if (w instanceof QuestPositionableButton pos && !(w instanceof Stand)) {
                Movable m = pos.moveAndDeleteFocus();
                boolean hidden = m instanceof ChapterImage ? ClusterViewState.isHiddenImage(m.getMovableID()) : ClusterViewState.isHidden(m.getMovableID());
                if (hidden) {
                    changed = true;
                    ClusterBoxWidget box = ownerBox(m, boxes);
                    if (box != null) {
                        out.add(new Stand(panel, m, box));
                    }
                    continue;
                }
            }
            out.add(w);
        }
        if (changed) {
            swaps++;
        }
        return changed ? out : widgets;
    }

    private static ClusterBoxWidget ownerBox(Movable m, Map<Long, ClusterBoxWidget> boxes) {
        var owner = ClientClusters.store().clusterOfMember(m.getMovableID());
        if (owner.isPresent()) {
            return boxes.get(owner.get().id());
        }
        // anchor point of a collapsed cluster: attach to whichever collapsed box owns it
        for (ClusterBoxWidget b : boxes.values()) {
            QuestCluster c = b.cluster;
            if (com.questclusters.compat.QuestEnhanceCompat.anchorsInside(
                    dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE.getChapter(c.chapterId()), c.members()).contains(m.getMovableID())) {
                return b;
            }
        }
        return null;
    }

    /** Stand-in for a hidden object, occupying the cluster box's rectangle. Never added to the panel. */
    static final class Stand extends Widget implements QuestPositionableButton {
        private final Movable movable;

        Stand(Panel panel, Movable movable, ClusterBoxWidget box) {
            super(panel);
            this.movable = movable;
            setPosAndSize(box.posX, box.posY, box.width, box.height);
        }

        @Override
        public Position getPosition() {
            return new Position(movable.getX(), movable.getY(), movable.getWidth(), movable.getHeight());
        }

        @Override
        public Movable moveAndDeleteFocus() {
            return movable;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        }
    }
}
