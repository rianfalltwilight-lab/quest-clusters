package com.questclusters.client.gui;

import com.questclusters.client.ClientScoring;
import com.questclusters.data.ScoreCalc;
import com.questclusters.data.SettlementSettings;
import com.questclusters.platform.Rl;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Settlement page: a slowly scrolling summary in the style of end-of-game credits. Content is the author's
 * (title, intro/outro lines, node texts, rank names, images); totals come from the team's completions.
 * The theme decides the backdrop and how the parts appear: {@code credits} (plain scroller), {@code report}
 * (a report card with a stamped rank), {@code neon} (glowing text over a starfield), {@code terminal} (typed out
 * line by line) and {@code podium} (the rank badge zooms in with confetti).
 * Space / mouse held = faster, scroll wheel = move, Esc = close.
 */
public class SettlementScreen extends Screen {
    private static final float SPEED = 0.5F; // px per tick
    private final Screen previous;
    private final SettlementSettings override;
    private final List<Item> items = new ArrayList<>();
    private final List<float[]> confetti = new ArrayList<>(); // x, y, vx, vy, colour, spin
    private final Random random = new Random(7L);
    private SettlementSettings settings;
    private Theme theme;
    private float scroll;
    private boolean fast;
    private int contentHeight;
    private int age;

    private enum Theme { CREDITS, REPORT, NEON, TERMINAL, PODIUM }

    /** One part of the page. {@code shown} is the tick it first came into view (-1 = not yet). */
    private abstract static class Item {
        int height, gap;
        int shown = -1;

        abstract String plain();
    }

    private static final class Text extends Item {
        final FormattedCharSequence text;
        final float scale;
        final boolean title;

        Text(FormattedCharSequence text, float scale, int height, int gap, boolean title) {
            this.text = text;
            this.scale = scale;
            this.height = height;
            this.gap = gap;
            this.title = title;
        }

        String plain() {
            return SettlementScreen.plain(text);
        }
    }

    private static final class Image extends Item {
        final SettlementSettings.ImageLine image;
        final int w;

        Image(SettlementSettings.ImageLine image, int w, int h, int gap) {
            this.image = image;
            this.w = w;
            this.height = h;
            this.gap = gap;
        }

        String plain() {
            return "[image " + image.texture() + "]";
        }
    }

    /** Report card row: text on the left, points on the right. */
    private static final class Row extends Item {
        final FormattedCharSequence left;
        final Component right;

        Row(FormattedCharSequence left, Component right, int height, int gap) {
            this.left = left;
            this.right = right;
            this.height = height;
            this.gap = gap;
        }

        String plain() {
            return SettlementScreen.plain(left) + " " + right.getString();
        }
    }

    /** The total; counts up once in view for the report, neon and podium themes. */
    private static final class Total extends Item {
        final int total, max;
        final double percent;
        final boolean showMax;

        Total(int total, int max, double percent, boolean showMax, int height, int gap) {
            this.total = total;
            this.max = max;
            this.percent = percent;
            this.showMax = showMax;
            this.height = height;
            this.gap = gap;
        }

        Component text(float f) {
            int t = Math.round(total * f);
            return showMax ? Component.translatable("quest_clusters.settlement.total", t, max, String.format("%.1f", percent * f))
                    : Component.translatable("quest_clusters.settlement.total_nomax", t);
        }

        String plain() {
            return text(1F).getString();
        }
    }

    private static final class RankItem extends Item {
        final Component name;
        final String badge;
        final int badgeSize;

        RankItem(Component name, String badge, int badgeSize, int height, int gap) {
            this.name = name;
            this.badge = badge;
            this.badgeSize = badgeSize;
            this.height = height;
            this.gap = gap;
        }

        String plain() {
            return name.getString();
        }
    }

    public SettlementScreen(Screen previous) {
        this(previous, null);
    }

    /** @param override settings to show instead of the synced ones (editor preview, self-test) */
    public SettlementScreen(Screen previous, SettlementSettings override) {
        super(Component.translatable("quest_clusters.settlement.screen"));
        this.previous = previous;
        this.override = override;
    }

    @Override
    protected void init() {
        items.clear();
        settings = override != null ? override : ClientScoring.settings();
        SettlementSettings s = settings;
        theme = Theme.values()[Math.max(0, SettlementSettings.THEMES.indexOf(s.theme()))];
        ScoreCalc.Result r = ClientScoring.myResult();
        int wrap = contentWidth() - 20;

        line(s.title(), 2F, 24, wrap, true);
        var td = ClientQuestFile.INSTANCE != null ? ClientQuestFile.INSTANCE.selfTeamData : null;
        Component team = null;
        var teams = dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api();
        if (teams.isClientManagerLoaded() && teams.getClientManager().selfTeam() != null) {
            team = teams.getClientManager().selfTeam().getName(); // display name, like the leaderboard
        } else if (td != null) {
            team = Component.literal(td.getName());
        }
        if (team != null) add(team.copy().withStyle(ChatFormatting.GRAY), 1F, 24, wrap, false);
        for (String l : s.intro()) line(l, 1F, 6, wrap, false);
        if (!s.intro().isEmpty()) gap(18);

        for (ScoreCalc.Node n : r.nodes()) {
            Component pts = n.score() == 0 ? null : Component.translatable("quest_clusters.settlement.points", (n.score() > 0 ? "+" : "") + n.score())
                    .withStyle(n.score() > 0 ? ChatFormatting.GREEN : ChatFormatting.RED);
            if (theme == Theme.REPORT && SettlementSettings.ImageLine.parse(n.text()) == null) {
                // one row per node: the text wrapped in the left part, points on the last line's right
                int right = pts == null ? 0 : font.width(pts) + 12;
                List<FormattedCharSequence> split = font.split(style(ClusterText.text(n.text())), Math.max(20, wrap - right));
                for (int i = 0; i < split.size(); i++) {
                    boolean last = i == split.size() - 1;
                    items.add(new Row(split.get(i), last && pts != null ? pts : Component.empty(), font.lineHeight, last ? 6 : 1));
                }
                continue;
            }
            line(n.text(), 1.2F, 2, wrap, false);
            if (pts != null) add(pts, 0.9F, 10, wrap, false);
            else gap(10);
        }
        if (r.nodes().isEmpty()) add(Component.translatable("quest_clusters.settlement.no_nodes").withStyle(ChatFormatting.DARK_GRAY), 1F, 10, wrap, false);
        gap(20);

        if (!r.perChapter().isEmpty()) {
            add(Component.translatable("quest_clusters.settlement.chapters").withStyle(ChatFormatting.GOLD), 1.1F, 8, wrap, false);
            r.perChapter().forEach((chapterId, v) -> {
                var ch = ClientQuestFile.INSTANCE.getChapter(chapterId);
                Component name = ch != null ? ch.getTitle() : Component.literal("?");
                add(s.showMax() ? Component.translatable("quest_clusters.settlement.chapter_line", name, v[0], v[1])
                        : Component.translatable("quest_clusters.settlement.chapter_line_nomax", name, v[0]), 1F, 3, wrap, false);
            });
            gap(20);
        }

        items.add(new Total(r.total(), r.max(), r.percent(), s.showMax(), (int) (font.lineHeight * 1.4F), 10));
        SettlementSettings.Rank rank = s.showMax() && r.max() > 0 ? s.rankOf(r.percent()) : null;
        if (rank != null && (!rank.name().isEmpty() || !rank.image().isEmpty())) {
            add(Component.translatable("quest_clusters.settlement.rank"), 1F, 4, wrap, false);
            int badge = rank.image().isEmpty() ? 0 : rank.size();
            int h = badge + (rank.name().isEmpty() ? 0 : (int) (font.lineHeight * 3F) + (badge > 0 ? 6 : 0));
            items.add(new RankItem(style(ClusterText.text(rank.name())), rank.image(), badge, h, 24));
        }
        for (String l : s.outro()) line(l, 1F, 6, wrap, false);

        contentHeight = 0;
        for (Item i : items) contentHeight += i.height + i.gap;
        scroll = -height * 0.6F;
    }

    private int contentWidth() {
        return Math.min(width - 40, theme == Theme.REPORT ? 380 : 360);
    }

    /** An author line: an image ({image:…}) or text. */
    private void line(String raw, float scale, int gap, int wrap, boolean title) {
        SettlementSettings.ImageLine img = SettlementSettings.ImageLine.parse(raw);
        if (img == null) {
            add(ClusterText.text(raw), scale, gap, wrap, title);
            return;
        }
        int w = img.width(), h = img.height();
        var rl = com.questclusters.client.Textures.file(img.texture());
        int[] size = rl == null ? null : com.questclusters.client.Textures.size(rl);
        int tw = size == null ? 64 : frameWidth(rl, size), th = size == null ? 64 : size[2]; // one frame of an animation
        if (w <= 0 && h <= 0) {
            w = tw;
            h = th;
        } else if (h <= 0) {
            h = Math.max(1, w * th / Math.max(1, tw));
        } else if (w <= 0) {
            w = Math.max(1, h * tw / Math.max(1, th));
        }
        if (w > wrap) {
            h = h * wrap / w;
            w = wrap;
        }
        items.add(new Image(img, w, h, gap));
    }

    private Component style(Component c) {
        return theme == Theme.TERMINAL ? c.copy().withStyle(st -> st.withFont(Rl.of("minecraft", "uniform"))) : c;
    }

    private void add(Component c, float scale, int gap, int wrap, boolean title) {
        List<FormattedCharSequence> split = font.split(style(c), Math.max(20, (int) (wrap / scale)));
        for (int i = 0; i < split.size(); i++) {
            items.add(new Text(split.get(i), scale, (int) (font.lineHeight * scale), i == split.size() - 1 ? gap : 1, title));
        }
    }

    private void gap(int px) {
        items.add(new Text(FormattedCharSequence.EMPTY, 1F, font.lineHeight, px, false));
    }

    @Override
    public void tick() {
        age++;
        float max = contentHeight - height * 0.4F;
        if (scroll < max) {
            scroll = Math.min(max, scroll + (fast ? SPEED * 6F : SPEED));
        }
        for (int i = confetti.size() - 1; i >= 0; i--) {
            float[] p = confetti.get(i);
            p[0] += p[2];
            p[1] += p[3];
            p[2] *= 0.98F;
            p[3] += 0.12F;
            p[5] += 0.2F;
            if (p[1] > height + 10) confetti.remove(i);
        }
    }

    /** For the dev self-test. */
    public int lineCount() {
        return items.size();
    }

    public List<String> plainLines() {
        List<String> out = new ArrayList<>();
        for (Item i : items) out.add(i.plain());
        return out;
    }

    /** For the dev self-test: jumps to the end and shows every part as if it had been in view for a while. */
    public void skipToEnd() {
        scroll = Math.max(-height * 0.6F, contentHeight - height * 0.9F);
        age = Math.max(age, 200);
        for (Item i : items) {
            if (i.shown < 0) {
                i.shown = 0;
                if (i instanceof RankItem) reveal((RankItem) i, width / 2F, height / 2F);
            }
        }
    }

    public String theme() {
        return settings.theme();
    }

    private static String plain(FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((i, style, cp) -> {
            sb.appendCodePoint(cp);
            return true;
        });
        return sb.toString();
    }

    /** The first {@code n} characters of a line (typewriter). */
    private static FormattedCharSequence prefix(FormattedCharSequence seq, int n) {
        return sink -> {
            int[] c = {0};
            return seq.accept((i, st, cp) -> c[0]++ < n && sink.accept(i, st, cp));
        };
    }

    /** The same line with every colour removed, so a glow pass draws in one colour. */
    private static FormattedCharSequence uncoloured(FormattedCharSequence seq) {
        return sink -> seq.accept((i, st, cp) -> sink.accept(i, st.withColor((TextColor) null), cp));
    }

    private static int length(FormattedCharSequence seq) {
        int[] c = {0};
        seq.accept((i, st, cp) -> {
            c[0]++;
            return true;
        });
        return c[0];
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float t = age + partialTick;
        backdrop(g, t);
        float y = -scroll - partialTick * (fast ? SPEED * 6F : SPEED);
        int cw = contentWidth(), left = (width - cw) / 2;
        if (theme == Theme.REPORT) paper(g, left - 10, (int) y - 20, cw + 20, contentHeight + 40);
        int typing = -1; // terminal: the line being typed gets the cursor
        float typingX = 0, typingY = 0;
        for (Item item : items) {
            if (y + item.height > -40 && y < height + 40) {
                if (item.shown < 0 && y < height * 0.85F) {
                    item.shown = age;
                    if (item instanceof RankItem) reveal((RankItem) item, width / 2F, y + item.height / 2F);
                }
                float since = item.shown < 0 ? 0F : t - item.shown;
                if (item instanceof Text) {
                    Text tx = (Text) item;
                    FormattedCharSequence seq = tx.text;
                    if (theme == Theme.TERMINAL) {
                        int len = length(seq), n = Mth.floor(since * 2.5F);
                        if (n < len) {
                            seq = prefix(seq, n);
                            typing = 1;
                        }
                        int w = (int) (font.width(seq) * tx.scale);
                        float x = (width - (int) (font.width(tx.text) * tx.scale)) / 2F;
                        text(g, seq, x, y, tx.scale, tx.title, t);
                        if (typing == 1) {
                            typing = 0;
                            typingX = x + w;
                            typingY = y;
                        }
                    } else {
                        int w = (int) (font.width(seq) * tx.scale);
                        text(g, seq, (width - w) / 2F, y, tx.scale, tx.title, t);
                    }
                } else if (item instanceof Image) {
                    Image im = (Image) item;
                    int x = im.image.align() == SettlementSettings.ImageLine.LEFT ? left
                            : im.image.align() == SettlementSettings.ImageLine.RIGHT ? left + cw - im.w : (width - im.w) / 2;
                    float a = theme == Theme.CREDITS ? 1F : Mth.clamp(since / 10F, 0F, 1F);
                    ClusterRender.textureFill(g, im.image.texture(), com.questclusters.data.ClusterStyle.MODE_STRETCH, 0,
                            x, (int) y, im.w, im.height, 1F, a);
                } else if (item instanceof Row) {
                    Row row = (Row) item;
                    text(g, row.left, left + 4, y, 1F, false, t);
                    int rw = font.width(row.right);
                    if (rw > 0) {
                        int lw = font.width(row.left);
                        for (int dx = left + 8 + lw; dx < left + cw - rw - 8; dx += 4) g.fill(dx, (int) y + 7, dx + 1, (int) y + 8, 0x60503820);
                        g.drawString(font, row.right, left + cw - rw - 4, (int) y, 0xFF3B2A1A, false);
                    }
                } else if (item instanceof Total) {
                    Total tot = (Total) item;
                    float f = theme == Theme.CREDITS || theme == Theme.TERMINAL ? 1F : ease(Mth.clamp(since / 40F, 0F, 1F));
                    FormattedCharSequence seq = style(tot.text(f)).getVisualOrderText();
                    text(g, seq, (width - font.width(seq) * 1.4F) / 2F, y, 1.4F, theme == Theme.NEON, t);
                } else if (item instanceof RankItem) {
                    rank(g, (RankItem) item, y, since, t);
                }
            }
            y += item.height + item.gap;
        }
        if (typing == 0 && (age / 8) % 2 == 0) g.fill((int) typingX + 1, (int) typingY, (int) typingX + 6, (int) typingY + 8, 0xFF33FF66);
        for (float[] p : confetti) {
            g.pose().pushPose();
            g.pose().translate(p[0], p[1], 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotation(p[5]));
            g.fill(-2, -1, 2, 1, (int) p[4]);
            g.pose().popPose();
        }
        overlay(g, t);
        // controls hint for the first few seconds only, so it never sits on top of the scrolling text for long
        int alpha = age < 80 ? 0xC0 : Math.max(0, 0xC0 - (age - 80) * 8);
        if (alpha > 8) {
            g.fill(0, height - 16, width, height, (alpha / 2) << 24);
            g.drawCenteredString(font, Component.translatable("quest_clusters.settlement.hint"), width / 2, height - 12, (alpha << 24) | 0x999999);
        }
    }

    private static float ease(float f) {
        return 1F - (1F - f) * (1F - f) * (1F - f);
    }

    /** One line of text in the theme's colour and effect. */
    private void text(GuiGraphics g, FormattedCharSequence seq, float x, float y, float scale, boolean title, float t) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1F);
        switch (theme) {
            case REPORT -> g.drawString(font, seq, 0, 0, title ? 0xFF5A2E0C : 0xFF3B2A1A, false);
            case TERMINAL -> g.drawString(font, seq, 0, 0, 0xFF33FF66, false);
            case NEON -> {
                int glow = title ? Mth.hsvToRgb((t / 120F) % 1F, 0.7F, 1F) : 0x40E0FF;
                FormattedCharSequence flat = uncoloured(seq);
                for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) g.drawString(font, flat, d[0], d[1], 0x50000000 | glow, false);
                g.drawString(font, seq, 0, 0, title ? 0xFF000000 | glow : 0xFFFFFFFF, false);
            }
            case PODIUM -> g.drawString(font, seq, 0, 0, title ? 0xFFFFD86B : 0xFFFFFFFF, true);
            default -> g.drawString(font, seq, 0, 0, 0xFFFFFFFF, true);
        }
        g.pose().popPose();
    }

    private void rank(GuiGraphics g, RankItem r, float y, float since, float t) {
        float s = 1F, rot = 0F, alpha = 1F;
        switch (theme) {
            case REPORT -> { // stamped: drops from large onto the paper, tilted
                float f = Mth.clamp(since / 6F, 0F, 1F);
                s = 1F + (1F - f) * 2F;
                alpha = f;
                rot = -0.26F;
            }
            case PODIUM -> { // zooms in with an overshoot
                float f = Mth.clamp(since / 14F, 0F, 1F);
                s = f < 0.7F ? f / 0.7F * 1.25F : 1.25F - (f - 0.7F) / 0.3F * 0.25F;
                alpha = Mth.clamp(since / 4F, 0F, 1F);
            }
            case NEON -> s = 1F + 0.05F * Mth.sin(t / 6F);
            default -> {
            }
        }
        if (s <= 0.01F) return;
        float cx = width / 2F, cy = y + r.height / 2F;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotation(rot));
        g.pose().scale(s, s, 1F);
        float top = -r.height / 2F;
        if (theme == Theme.PODIUM) { // light behind the badge
            for (int i = 0; i < 8; i++) {
                g.pose().pushPose();
                g.pose().mulPose(com.mojang.math.Axis.ZP.rotation(t / 40F + i * Mth.PI / 4F));
                g.fillGradient(-3, 0, 3, (int) (r.height * 0.9F + 20), ((int) (alpha * 0x50) << 24) | 0xFFD86B, 0x00FFD86B);
                g.pose().popPose();
            }
        }
        if (r.badgeSize > 0) {
            var rl = com.questclusters.client.Textures.file(r.badge);
            int[] size = rl == null ? null : com.questclusters.client.Textures.size(rl);
            int bh = r.badgeSize, bw = size == null ? bh : Math.max(1, bh * frameWidth(rl, size) / Math.max(1, size[2]));
            ClusterRender.textureFill(g, r.badge, com.questclusters.data.ClusterStyle.MODE_STRETCH, 0, -bw / 2, (int) top, bw, bh, 1F, alpha);
            top += bh + 6;
        }
        if (length(r.name.getVisualOrderText()) > 0) {
            FormattedCharSequence seq = r.name.getVisualOrderText();
            int w = font.width(seq);
            g.pose().translate(0, top, 0);
            g.pose().scale(3F, 3F, 1F);
            int a = Math.max(4, (int) (alpha * 255)) << 24;
            if (theme == Theme.REPORT) {
                int c = a | 0xC0392B;
                g.renderOutline(-w / 2 - 3, -2, w + 6, font.lineHeight + 3, c);
                g.drawString(font, uncoloured(seq), -w / 2, 0, c, false);
            } else {
                if (theme == Theme.NEON) {
                    for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                        g.drawString(font, uncoloured(seq), -w / 2 + d[0], d[1], 0x50000000 | 0xFF40FF, false);
                    }
                }
                g.drawString(font, seq, -w / 2, 0, a | (theme == Theme.TERMINAL ? 0x33FF66 : 0xFFFFFF), theme != Theme.NEON);
            }
        }
        g.pose().popPose();
    }

    private static int frameWidth(net.minecraft.resources.ResourceLocation rl, int[] size) {
        var f = com.questclusters.client.Textures.frame(rl, 0L);
        return f == null ? size[0] : Math.max(1, Math.round((f.u1() - f.u0()) * size[0]));
    }

    /** The rank came into view: confetti and a sound for the podium, a thump for the report card stamp. */
    private void reveal(RankItem r, float x, float y) {
        var sounds = minecraft == null ? null : minecraft.getSoundManager();
        if (theme == Theme.PODIUM) {
            int[] colours = {0xFFFFD86B, 0xFFFF6B9A, 0xFF6BD7FF, 0xFF8CFF6B, 0xFFFFFFFF, 0xFFC08CFF};
            for (int i = 0; i < 90; i++) {
                double a = random.nextDouble() * Math.PI * 2, v = 2 + random.nextDouble() * 5;
                confetti.add(new float[]{x, y, (float) (Math.cos(a) * v), (float) (Math.sin(a) * v - 3), colours[i % colours.length],
                        random.nextFloat() * 6F});
            }
            if (sounds != null) sounds.play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1F, 0.6F));
        } else if (theme == Theme.REPORT && sounds != null) {
            sounds.play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 1.6F, 0.25F));
        }
    }

    private void backdrop(GuiGraphics g, float t) {
        switch (theme) {
            case REPORT -> g.fillGradient(0, 0, width, height, 0xFF2B1D14, 0xFF1A110B);
            case NEON -> {
                g.fillGradient(0, 0, width, height, 0xFF070818, 0xFF1A0B2E);
                stars(g, t);
            }
            case TERMINAL -> g.fill(0, 0, width, height, 0xFF050A06);
            case PODIUM -> {
                g.fillGradient(0, 0, width, height, 0xFF120818, 0xFF000000);
                int w = Math.min(width, 260);
                g.fillGradient((width - w) / 2, 0, (width + w) / 2, height, 0x26FFE9A8, 0x00FFE9A8);
            }
            default -> g.fill(0, 0, width, height, 0xF0000000);
        }
        if (!settings.background().isEmpty()) {
            ClusterRender.textureFill(g, settings.background(), settings.backgroundMode(), 8, 0, 0, width, height, 1F, 1F);
        }
    }

    /** Three layers of stars, the far ones moving slower with the scroll. */
    private void stars(GuiGraphics g, float t) {
        Random r = new Random(42L);
        for (int i = 0; i < 160; i++) {
            int layer = 1 + i % 3;
            float x = r.nextFloat() * width;
            float y = ((r.nextFloat() * height * 2 - scroll * layer * 0.25F) % height + height) % height;
            float tw = 0.5F + 0.5F * Mth.sin(t / (8F + r.nextFloat() * 10F) + i);
            int a = (int) ((0.25F + 0.25F * layer) * tw * 255);
            int s = layer == 3 && i % 7 == 0 ? 2 : 1;
            g.fill((int) x, (int) y, (int) x + s, (int) y + s, (a << 24) | (i % 5 == 0 ? 0xFFB0F0 : 0xDDEEFF));
        }
    }

    private void paper(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 4, y + 4, x + w + 4, y + h + 4, 0x60000000);
        g.fill(x, y, x + w, y + h, 0xFFF3E9D2);
        g.renderOutline(x, y, w, h, 0xFF8B6F47);
        g.renderOutline(x + 3, y + 3, w - 6, h - 6, 0x608B6F47);
        for (int ly = y + 30; ly < y + h - 10; ly += 12) {
            if (ly > -2 && ly < height + 2) g.fill(x + 8, ly, x + w - 8, ly + 1, 0x18503820);
        }
    }

    /** Edge fades for the scrollers, scanlines for the terminal. */
    private void overlay(GuiGraphics g, float t) {
        if (theme == Theme.TERMINAL) {
            for (int y = 0; y < height; y += 3) g.fill(0, y, width, y + 1, 0x40000000);
            int band = (int) ((t * 2) % (height + 60)) - 30;
            g.fillGradient(0, band, width, band + 30, 0x0000FF66, 0x1000FF66);
        }
        int edge = theme == Theme.REPORT ? 0xFF1A110B : theme == Theme.NEON ? 0xFF070818 : 0xFF000000;
        g.fillGradient(0, 0, width, 40, edge, edge & 0x00FFFFFF);
        g.fillGradient(0, height - 40, width, height, edge & 0x00FFFFFF, theme == Theme.NEON ? 0xFF1A0B2E : edge);
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

    //? if neoforge {
    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
    //?} else {
    /*@Override
    public boolean mouseScrolled(double x, double y, double dy) {
    *///?}
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
