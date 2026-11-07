package com.questclusters.client.gui;

import com.questclusters.data.QuestCluster;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import net.minecraft.client.gui.screens.Screen;

/**
 * Shared mouse handling for the box and the frame strip.
 * Players: left click toggles. Editors: left click toggles, left drag moves the whole cluster
 * (box and members, snapped to 1/8 of a grid cell unless Shift is held), right click opens the cluster menu.
 */
abstract class ClusterWidgetBase extends Widget {
    private static final int DRAG_THRESHOLD = 3;

    protected final QuestPanel questPanel;
    final QuestCluster cluster;
    private boolean pressed;
    private boolean dragging;
    private int pressX, pressY;

    ClusterWidgetBase(QuestPanel panel, QuestCluster cluster) {
        super(panel);
        this.questPanel = panel;
        this.cluster = cluster;
    }

    /** 0..1: how far the expand/collapse transition that made this widget visible has progressed. */
    protected float appear() {
        return Anim.progress(com.questclusters.client.ClusterViewState.lastChange(cluster.id()), 180F);
    }

    /** What a plain left click does. */
    protected abstract void onToggle();

    @Override
    public boolean mousePressed(MouseButton button) {
        if (!isMouseOver()) {
            return false;
        }
        boolean edit = ClusterWidgets.canEdit();
        if (button.isLeft()) {
            if (edit) {
                pressed = true;
                dragging = false;
                pressX = getMouseX();
                pressY = getMouseY();
            } else {
                playClickSound();
                onToggle();
            }
            return true;
        }
        if (button.isRight() && edit) {
            playClickSound();
            getGui().openContextMenu(ClusterEditor.clusterMenu(ClusterWidgets.screen(questPanel), cluster));
            return true;
        }
        // swallow other clicks so FTB Quests' "create quest here" menu never opens under the box
        return true;
    }

    @Override
    public void mouseReleased(MouseButton button) {
        if (!pressed || !button.isLeft()) {
            return;
        }
        pressed = false;
        if (dragging || movedPastThreshold()) {
            dragging = false;
            double[] d = canvasDelta();
            ClusterEditor.moveBy(ClusterWidgets.screen(questPanel), cluster, d[0], d[1]);
        } else {
            playClickSound();
            onToggle();
        }
    }

    /** Pixel offset to draw the widget at while it is being dragged. */
    protected int[] dragOffsetPx() {
        if (pressed && !dragging && movedPastThreshold()) {
            dragging = true;
        }
        if (!dragging) {
            return new int[]{0, 0};
        }
        double[] d = canvasDelta();
        double cell = cellSize();
        return new int[]{(int) Math.round(d[0] * cell), (int) Math.round(d[1] * cell)};
    }

    private boolean movedPastThreshold() {
        return Math.abs(getMouseX() - pressX) > DRAG_THRESHOLD || Math.abs(getMouseY() - pressY) > DRAG_THRESHOLD;
    }

    boolean isDragging() {
        return dragging;
    }

    private double cellSize() {
        var qs = ClusterWidgets.screen(questPanel);
        return qs.getQuestButtonSize() + qs.getQuestButtonSpacing();
    }

    private double[] canvasDelta() {
        double cell = cellSize();
        double dx = (getMouseX() - pressX) / cell;
        double dy = (getMouseY() - pressY) / cell;
        if (!Screen.hasShiftDown()) {
            dx = Math.round(dx * 8D) / 8D;
            dy = Math.round(dy * 8D) / 8D;
        }
        return new double[]{dx, dy};
    }
}
