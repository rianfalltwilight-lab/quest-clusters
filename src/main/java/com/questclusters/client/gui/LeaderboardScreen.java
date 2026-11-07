package com.questclusters.client.gui;

import com.questclusters.data.ScoreCalc;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Leaderboard: teams by completed quests, then score. Data comes from the server when opened. */
public class LeaderboardScreen extends Screen {
    private final Screen previous;
    private final List<ScoreCalc.Row> rows;
    private int scroll;

    public LeaderboardScreen(Screen previous, List<ScoreCalc.Row> rows) {
        super(Component.translatable("quest_clusters.leaderboard.title"));
        this.previous = previous;
        this.rows = rows;
    }

    public Screen previous() {
        return previous;
    }

    public List<ScoreCalc.Row> rows() {
        return rows;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xE0101018);
        int w = Math.min(360, width - 20), x0 = (width - w) / 2;
        g.drawCenteredString(font, getTitle().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), width / 2, 12, 0xFFFFFFFF);
        int y = 32;
        g.drawString(font, Component.translatable("quest_clusters.leaderboard.col_rank").withStyle(ChatFormatting.GRAY), x0, y, 0xFFAAAAAA);
        g.drawString(font, Component.translatable("quest_clusters.leaderboard.col_team").withStyle(ChatFormatting.GRAY), x0 + 34, y, 0xFFAAAAAA);
        g.drawString(font, Component.translatable("quest_clusters.leaderboard.col_quests").withStyle(ChatFormatting.GRAY), x0 + w - 110, y, 0xFFAAAAAA);
        g.drawString(font, Component.translatable("quest_clusters.leaderboard.col_score").withStyle(ChatFormatting.GRAY), x0 + w - 45, y, 0xFFAAAAAA);
        y += 14;
        g.fill(x0, y - 3, x0 + w, y - 2, 0x40FFFFFF);
        int visible = (height - y - 20) / 13;
        for (int i = scroll; i < Math.min(rows.size(), scroll + visible); i++) {
            ScoreCalc.Row r = rows.get(i);
            if (r.own()) g.fill(x0 - 2, y - 2, x0 + w + 2, y + 10, 0x40FFD700);
            int color = i == 0 ? 0xFFFFD700 : i == 1 ? 0xFFC0C0C0 : i == 2 ? 0xFFCD7F32 : 0xFFFFFFFF;
            g.drawString(font, "#" + (i + 1), x0, y, color);
            g.drawString(font, font.plainSubstrByWidth(r.team(), w - 150), x0 + 34, y, 0xFFFFFFFF);
            g.drawString(font, Integer.toString(r.quests()), x0 + w - 110, y, 0xFFFFFFFF);
            g.drawString(font, Integer.toString(r.score()), x0 + w - 45, y, 0xFFFFFFFF);
            y += 13;
        }
        if (rows.isEmpty()) g.drawCenteredString(font, Component.translatable("quest_clusters.leaderboard.empty"), width / 2, y + 10, 0xFF888888);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        scroll = Math.max(0, Math.min(Math.max(0, rows.size() - 5), scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
