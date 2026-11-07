package com.questclusters.client.gui;

import com.questclusters.client.ClientScoring;
import com.questclusters.data.ScoreCalc;
import com.questclusters.data.SettlementSettings;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Settlement page: a slowly scrolling summary in the style of end-of-game credits. Content is the author's
 * (title, intro/outro lines, node texts, rank names); totals come from the team's completions.
 * Space / mouse held = faster, scroll wheel = move, Esc = close.
 */
public class SettlementScreen extends Screen {
    private static final float SPEED = 0.5F; // px per tick
    private final Screen previous;
    private final List<Line> lines = new ArrayList<>();
    private float scroll;
    private boolean fast;
    private int contentHeight;
    private int age;

    private record Line(FormattedCharSequence text, float scale, int gap) {
    }

    public SettlementScreen(Screen previous) {
        super(Component.translatable("quest_clusters.settlement.screen"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        lines.clear();
        SettlementSettings s = ClientScoring.settings();
        ScoreCalc.Result r = ClientScoring.myResult();
        int wrap = Math.min(width - 40, 360);

        add(ClusterText.text(s.title()), 2F, 24, wrap);
        var td = ClientQuestFile.INSTANCE != null ? ClientQuestFile.INSTANCE.selfTeamData : null;
        Component team = null;
        var teams = dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api();
        if (teams.isClientManagerLoaded() && teams.getClientManager().selfTeam() != null) {
            team = teams.getClientManager().selfTeam().getName(); // display name, like the leaderboard
        } else if (td != null) {
            team = Component.literal(td.getName());
        }
        if (team != null) add(team.copy().withStyle(ChatFormatting.GRAY), 1F, 24, wrap);
        for (String l : s.intro()) add(ClusterText.text(l), 1F, 6, wrap);
        if (!s.intro().isEmpty()) gap(18);

        for (ScoreCalc.Node n : r.nodes()) {
            add(ClusterText.text(n.text()), 1.2F, 2, wrap);
            if (n.score() != 0) {
                add(Component.translatable("quest_clusters.settlement.points", (n.score() > 0 ? "+" : "") + n.score())
                        .withStyle(n.score() > 0 ? ChatFormatting.GREEN : ChatFormatting.RED), 0.9F, 10, wrap);
            } else {
                gap(10);
            }
        }
        if (r.nodes().isEmpty()) add(Component.translatable("quest_clusters.settlement.no_nodes").withStyle(ChatFormatting.DARK_GRAY), 1F, 10, wrap);
        gap(20);

        if (!r.perChapter().isEmpty()) {
            add(Component.translatable("quest_clusters.settlement.chapters").withStyle(ChatFormatting.GOLD), 1.1F, 8, wrap);
            r.perChapter().forEach((chapterId, v) -> {
                var ch = ClientQuestFile.INSTANCE.getChapter(chapterId);
                Component name = ch != null ? ch.getTitle() : Component.literal("?");
                add(s.showMax() ? Component.translatable("quest_clusters.settlement.chapter_line", name, v[0], v[1])
                        : Component.translatable("quest_clusters.settlement.chapter_line_nomax", name, v[0]), 1F, 3, wrap);
            });
            gap(20);
        }

        add(s.showMax()
                ? Component.translatable("quest_clusters.settlement.total", r.total(), r.max(), String.format("%.1f", r.percent()))
                : Component.translatable("quest_clusters.settlement.total_nomax", r.total()), 1.4F, 10, wrap);
        String rank = s.showMax() && r.max() > 0 ? s.rankFor(r.percent()) : "";
        if (!rank.isEmpty()) {
            add(Component.translatable("quest_clusters.settlement.rank"), 1F, 4, wrap);
            add(ClusterText.text(rank), 3F, 24, wrap);
        }
        for (String l : s.outro()) add(ClusterText.text(l), 1F, 6, wrap);

        contentHeight = 0;
        for (Line l : lines) contentHeight += (int) (font.lineHeight * l.scale()) + l.gap();
        scroll = -height * 0.6F;
    }

    private void add(Component c, float scale, int gap, int wrap) {
        List<FormattedCharSequence> split = font.split(c, Math.max(20, (int) (wrap / scale)));
        for (int i = 0; i < split.size(); i++) {
            lines.add(new Line(split.get(i), scale, i == split.size() - 1 ? gap : 1));
        }
    }

    private void gap(int px) {
        lines.add(new Line(FormattedCharSequence.EMPTY, 1F, px));
    }

    @Override
    public void tick() {
        age++;
        float max = contentHeight - height * 0.4F;
        if (scroll < max) {
            scroll = Math.min(max, scroll + (fast ? SPEED * 6F : SPEED));
        }
    }

    /** For the dev self-test. */
    public int lineCount() {
        return lines.size();
    }

    public List<String> plainLines() {
        List<String> out = new ArrayList<>();
        for (Line l : lines) {
            StringBuilder sb = new StringBuilder();
            l.text().accept((i, style, cp) -> {
                sb.appendCodePoint(cp);
                return true;
            });
            out.add(sb.toString());
        }
        return out;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xF0000000);
        float y = -scroll - partialTick * (fast ? SPEED * 6F : SPEED);
        for (Line l : lines) {
            int h = (int) (font.lineHeight * l.scale());
            if (y + h > -20 && y < height + 20) {
                int w = (int) (font.width(l.text()) * l.scale());
                g.pose().pushPose();
                g.pose().translate((width - w) / 2F, y, 0);
                g.pose().scale(l.scale(), l.scale(), 1F);
                g.drawString(font, l.text(), 0, 0, 0xFFFFFFFF, true);
                g.pose().popPose();
            }
            y += h + l.gap();
        }
        // fade at the top and bottom edges
        g.fillGradient(0, 0, width, 40, 0xFF000000, 0x00000000);
        g.fillGradient(0, height - 40, width, height, 0x00000000, 0xFF000000);
        // controls hint for the first few seconds only, so it never sits on top of the scrolling text for long
        int alpha = age < 80 ? 0xC0 : Math.max(0, 0xC0 - (age - 80) * 8);
        if (alpha > 8) {
            g.fill(0, height - 16, width, height, (alpha / 2) << 24);
            g.drawCenteredString(font, Component.translatable("quest_clusters.settlement.hint"), width / 2, height - 12, (alpha << 24) | 0x999999);
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            fast = true;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE) fast = false;
        return super.keyReleased(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double x, double y, int b) {
        fast = true;
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int b) {
        fast = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        scroll = Math.max(-height * 0.6F, Math.min(contentHeight, scroll - (float) dy * 20F));
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
