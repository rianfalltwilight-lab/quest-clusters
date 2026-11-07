package com.questclusters.client.gui;

import com.questclusters.client.ClusterViewState;
import com.questclusters.data.QuestCluster;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.quest.Movable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The expanded form of a cluster: a translucent region behind its members with a title strip on top.
 * Only the title strip reacts to the mouse, so clicks and panning on the region still reach FTB Quests.
 */
public final class ClusterFrameWidget extends ClusterWidgetBase {
    private int headerH;
    private boolean hasArea;

    ClusterFrameWidget(QuestPanel panel, QuestCluster cluster) {
        super(panel, cluster);
        setDrawLayer(Widget.DrawLayer.BACKGROUND);
    }

    @Override
    protected void onToggle() {
        ClusterViewState.setExpanded(cluster, false);
    }

    void align() {
        List<Movable> shown = new ArrayList<>();
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        boolean edit = ClusterWidgets.canEdit();
        for (long id : cluster.members()) {
            if (file != null && file.getBase(id) instanceof Movable m) {
                if (edit || ClusterWidgets.isVisibleToPlayer(m)) {
                    shown.add(m);
                }
            }
        }
        int[] r = ClusterWidgets.boundsOf(questPanel, shown);
        hasArea = r != null;
        if (!hasArea) {
            setPosAndSize(0, 0, 0, 0);
            return;
        }
        int pad = (int) Math.max(3, ClusterWidgets.screen(questPanel).getQuestButtonSpacing() * 0.6 + 2);
        headerH = (int) Math.max(8, Math.min(16, ClusterWidgets.zoom(questPanel) * 0.75));
        setPosAndSize(r[0] - pad, r[1] - pad - headerH, r[2] + pad * 2, r[3] + pad * 2 + headerH);
    }

    @Override
    public boolean shouldDraw() {
        return hasArea && ClusterViewState.isExpanded(cluster);
    }

    @Override
    public boolean isEnabled() {
        return shouldDraw();
    }

    @Override
    public boolean checkMouseOver(int mouseX, int mouseY) {
        if (ClusterWidgets.blockedByOverlay(questPanel) || !super.checkMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (mouseY < getY() + headerH) {
            return true; // title strip
        }
        // in edit mode the frame's edges are drag handles too; the inside still lets clicks through for panning
        if (ClusterWidgets.canEdit()) {
            int e = EDGE;
            return mouseX < getX() + e || mouseX >= getX() + width - e || mouseY >= getY() + height - e;
        }
        return false;
    }

    private static final int EDGE = 4;

    @Override
    public dev.ftb.mods.ftblibrary.ui.CursorType getCursor() {
        return ClusterWidgets.canEdit() ? dev.ftb.mods.ftblibrary.ui.CursorType.MOVE : null;
    }

    @Override
    public void addMouseOverText(TooltipList list) {
        if (isMouseOver()) {
            list.add(ClusterText.title(cluster).copy().withStyle(ChatFormatting.BOLD));
            list.add(Component.translatable("quest_clusters.frame.collapse").withStyle(ChatFormatting.DARK_GRAY));
            if (ClusterWidgets.canEdit()) {
                list.add(Component.translatable("quest_clusters.edit_hint").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    @Override
    public void draw(GuiGraphics g, Theme theme, int x, int y, int w, int h) {
        int[] off = dragOffsetPx();
        if (off[0] != 0 || off[1] != 0) {
            // preview: outline at the drop position; members move once the drag is released
            g.pose().pushPose();
            g.pose().translate(off[0], off[1], 50);
            ClusterRender.outline(g, x, y, w, h, 0xC0FFFFFF, 1);
            g.fill(x, y, x + w, y + headerH, ClusterRender.withAlpha(cluster.color(), 160));
            g.pose().popPose();
        }
        float a = appear(); // fade in after expanding
        int base = cluster.style().frameColor() != 0 ? cluster.style().frameColor() : cluster.color();
        int edge = ClusterRender.lighten(ClusterRender.withAlpha(base, (int) (200 * a)), 0.35F);
        g.fill(x, y + headerH, x + w, y + h, ClusterRender.withAlpha(base, (int) (70 * a)));
        ClusterRender.outline(g, x, y, w, h, edge, ClusterWidgets.canEdit() && isMouseOver() ? 2 : 1);
        g.fill(x, y, x + w, y + headerH, ClusterRender.withAlpha(base, (int) ((isMouseOver() ? 255 : 220) * a)));

        float tx = x + 3;
        float avail = w - headerH - 6;
        // title left-aligned-ish: centre it within the strip minus the collapse glyph
        ClusterRender.centredText(g, ClusterText.title(cluster), tx + avail / 2F, y + headerH / 2F,
                headerH * 0.8F, avail, cluster.style().titleOr());
        ClusterRender.centredText(g, Component.literal("−"), x + w - headerH / 2F - 1, y + headerH / 2F,
                headerH * 0.9F, headerH, 0xFFFFFFFF);
    }
}
