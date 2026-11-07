package com.questclusters.client.gui;

import com.questclusters.client.ClusterViewState;
import com.questclusters.compat.CqaCompat;
import com.questclusters.data.QuestCluster;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The collapsed form of a cluster: a labelled rectangle standing in for all its members.
 */
public final class ClusterBoxWidget extends ClusterWidgetBase {
    private ClusterStatus status = ClusterStatus.EMPTY;
    private long statusTime;
    private final Anim hover = new Anim(0F, 120F);

    ClusterBoxWidget(QuestPanel panel, QuestCluster cluster) {
        super(panel, cluster);
    }

    @Override
    protected void onToggle() {
        ClusterViewState.setExpanded(cluster, true);
    }

    void align() {
        int[] r = ClusterWidgets.objectRect(questPanel, cluster.x(), cluster.y(), cluster.width(), cluster.height());
        setPosAndSize(r[0], r[1], Math.max(r[2], 4), Math.max(r[3], 4));
    }

    ClusterStatus status() {
        long now = System.currentTimeMillis();
        if (now - statusTime > 250L) {
            status = ClusterStatus.of(cluster);
            statusTime = now;
        }
        return status;
    }

    @Override
    public dev.ftb.mods.ftblibrary.ui.CursorType getCursor() {
        return ClusterWidgets.canEdit() ? dev.ftb.mods.ftblibrary.ui.CursorType.MOVE : null;
    }

    @Override
    public boolean shouldDraw() {
        return !ClusterViewState.isExpanded(cluster) && (ClusterWidgets.canEdit() || status().visible() > 0);
    }

    @Override
    public boolean isEnabled() {
        return shouldDraw();
    }

    @Override
    public boolean checkMouseOver(int mouseX, int mouseY) {
        return !ClusterWidgets.blockedByOverlay(questPanel) && super.checkMouseOver(mouseX, mouseY);
    }

    @Override
    public void addMouseOverText(TooltipList list) {
        if (!isMouseOver()) {
            return;
        }
        list.add(ClusterText.title(cluster).copy().withStyle(ChatFormatting.BOLD));
        if (!cluster.subtitle().isEmpty()) {
            list.add(ClusterText.subtitle(cluster).copy().withStyle(ChatFormatting.GRAY));
        }
        ClusterStatus s = status();
        if (s.visible() > 0) {
            list.add(Component.translatable("quest_clusters.box.progress", s.completed(), s.visible())
                    .withStyle(s.allComplete() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        }
        if (s.claimable()) {
            list.add(Component.translatable("quest_clusters.box.rewards").withStyle(ChatFormatting.GOLD));
        }
        if (ClusterWidgets.canEdit()) {
            list.add(Component.translatable("quest_clusters.box.members", cluster.members().size()).withStyle(ChatFormatting.DARK_GRAY));
        }
        list.add(Component.translatable("quest_clusters.box.expand").withStyle(ChatFormatting.DARK_GRAY));
        if (ClusterWidgets.canEdit()) {
            list.add(Component.translatable("quest_clusters.edit_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public void draw(GuiGraphics g, Theme theme, int x, int y, int w, int h) {
        int[] off = dragOffsetPx();
        if (off[0] != 0 || off[1] != 0) {
            g.pose().pushPose();
            g.pose().translate(off[0], off[1], 50);
            drawBox(g, x, y, w, h, 1F, 0F);
            g.pose().popPose();
            ClusterRender.outline(g, x, y, w, h, 0x80FFFFFF, 1); // where it was
            return;
        }
        // appear (after collapsing) and hover (CQA-style grow + glow) transitions
        float a = appear();
        float hv = hover.update(isMouseOver() ? 1F : 0F, true);
        float scale = (0.85F + 0.15F * a) * (CqaCompat.hoverAnimation() ? 1F + 0.06F * hv : 1F);
        if (scale != 1F) {
            g.pose().pushPose();
            g.pose().translate(x + w / 2F, y + h / 2F, 0);
            g.pose().scale(scale, scale, 1F);
            g.pose().translate(-(x + w / 2F), -(y + h / 2F), 0);
            drawBox(g, x, y, w, h, a, hv);
            g.pose().popPose();
        } else {
            drawBox(g, x, y, w, h, a, hv);
        }
    }

    private void drawBox(GuiGraphics g, int x, int y, int w, int h, float alpha, float hv) {
        ClusterStatus s = status();
        int base = cluster.color();
        int border = borderColor(s);
        int t = Math.max(1, (int) Math.round(ClusterWidgets.zoom(questPanel) / 10D));

        g.fill(x, y, x + w, y + h, ClusterRender.withAlpha(base, (int) (Math.max(200, base >>> 24) * alpha)));
        ClusterRender.outline(g, x, y, w, h, ClusterRender.withAlpha(border, (int) (255 * alpha)), t);
        if (hv > 0.01F) {
            g.fill(x, y, x + w, y + h, ClusterRender.withAlpha(0xFFFFFF, (int) (CqaCompat.hoverGlowAlpha() * 0.5F * hv)));
        }

        // layout: title row on top; bottom row = subtitle (left part) + progress (right part)
        boolean hasSub = !cluster.subtitle().isEmpty();
        boolean showProgress = s.visible() > 0;
        boolean twoRows = hasSub || showProgress;
        float pad = t + 2;
        float innerW = w - pad * 2;
        float titleH = Math.min(h * (twoRows ? 0.40F : 0.55F), 18F);
        float titleY = twoRows ? y + h * 0.38F : y + h / 2F;
        ClusterRender.centredText(g, ClusterText.title(cluster), x + w / 2F, titleY, titleH, innerW, cluster.style().titleOr());

        if (twoRows) {
            float rowH = Math.min(h * 0.24F, 10F);
            float rowY = y + h * 0.74F;
            float progressW = showProgress ? innerW * 0.28F : 0F;
            if (hasSub) {
                float subW = innerW - progressW - (showProgress ? 2 : 0);
                ClusterRender.centredText(g, ClusterText.subtitle(cluster), x + pad + subW / 2F, rowY,
                        rowH, subW, cluster.style().subtitleOr());
            }
            if (showProgress) {
                Component p = Component.literal(s.completed() + "/" + s.visible());
                float pcx = hasSub ? x + w - pad - progressW / 2F : x + w / 2F;
                ClusterRender.centredText(g, p, pcx, rowY, rowH, hasSub ? progressW : innerW,
                        s.allComplete() ? 0xFF7CFC7C : 0xFFDDDDDD);
                drawProgressBar(g, x, y, w, h, t, s);
            }
        }

        // same corner icons FTB Quests uses on quest buttons
        int is = (int) (Math.min(w, h) / 8F * 3F);
        Icon corner = s.claimable() ? ThemeProperties.ALERT_ICON.get() : s.allComplete() ? ThemeProperties.CHECK_ICON.get() : Icon.empty();
        if (!corner.isEmpty() && is >= 4) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 10);
            corner.draw(g, x + w - is + is / 4, y - is / 4, is, is);
            g.pose().popPose();
        }
        if (ClusterWidgets.canEdit() && s.anyHidden() && is >= 4) {
            ThemeProperties.HIDDEN_ICON.get().draw(g, x - is / 4, y - is / 4, is, is);
        }
    }

    private void drawProgressBar(GuiGraphics g, int x, int y, int w, int h, int t, ClusterStatus s) {
        int bh = Math.max(1, t);
        int bw = w - t * 2;
        int filled = (int) Math.round(bw * (s.completed() / (double) s.visible()));
        g.fill(x + t, y + h - t - bh, x + t + bw, y + h - t, 0x60000000);
        g.fill(x + t, y + h - t - bh, x + t + filled, y + h - t, ClusterRender.withAlpha(
                ThemeProperties.QUEST_COMPLETED_COLOR.get(ClusterWidgets.chapter(questPanel)).rgba(), 255));
    }

    private int borderColor(ClusterStatus s) {
        if (cluster.style().borderColor() != 0) {
            return cluster.style().borderColor();
        }
        var chapter = ClusterWidgets.chapter(questPanel);
        Color4I c;
        if (s.allComplete()) {
            c = ThemeProperties.QUEST_COMPLETED_COLOR.get(chapter);
        } else if (s.completed() > 0) {
            c = ThemeProperties.QUEST_STARTED_COLOR.get(chapter);
        } else if (s.startable() || s.visible() == 0) {
            c = ThemeProperties.QUEST_NOT_STARTED_COLOR.get(chapter);
        } else {
            c = ThemeProperties.QUEST_LOCKED_COLOR.get(chapter);
        }
        return ClusterRender.withAlpha(c.rgba(), 255);
    }
}
