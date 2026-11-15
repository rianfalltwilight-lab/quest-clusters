package com.questclusters.editor.client.desc;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Draws {@link LineMarkup} decorations around FTB Quests' description lines. Every description line is registered
 * with its quest and line number (also plain ones, for Shift+O); marked-up ones get a layout and decorations.
 */
public final class LineRender {
    public record Info(long questId, int line, LineMarkup.Line markup) {
    }

    private static final Map<TextField, Info> LINES = Collections.synchronizedMap(new WeakHashMap<>());
    /** Spoilers revealed this session: quest id + line number + text. */
    private static final Set<String> REVEALED = new HashSet<>();
    private static final int BAR = 0xFF4A90E2, DIVIDER = 0xFF8A8A8A, CODE_BG = 0xD0151515, SPOILER = 0xFF2A2A2A;
    public static final int BAR_PAD = 6, CALLOUT_PAD_LEFT = 7, BOX_PAD = 2;

    private LineRender() {
    }

    public static void register(TextField field, long questId, int line, LineMarkup.Line markup) {
        LINES.put(field, new Info(questId, line, markup));
    }

    /** The description line under the mouse in the given screen, if any. */
    public static Info hovered(dev.ftb.mods.ftblibrary.ui.BaseScreen screen) {
        synchronized (LINES) {
            for (var e : LINES.entrySet()) {
                TextField f = e.getKey();
                if (f != null && f.getGui() == screen && f.isMouseOver()) return e.getValue();
            }
        }
        return null;
    }

    /** For the dev self-test: the field showing a quest's description line. */
    public static TextField fieldFor(long questId, int line) {
        synchronized (LINES) {
            for (var e : LINES.entrySet()) {
                if (e.getKey() != null && e.getValue().questId() == questId && e.getValue().line() == line) return e.getKey();
            }
        }
        return null;
    }

    public static Info info(Widget w) {
        return w instanceof TextField tf ? LINES.get(tf) : null;
    }

    /** Left / right indent of the text inside the panel. */
    public static int indentLeft(LineMarkup.Line m, Theme theme) {
        int x = 0;
        if (m.barLeft()) x += BAR_PAD;
        if (m.callout() != null) x += CALLOUT_PAD_LEFT;
        if (m.code()) x += BOX_PAD + 1;
        if (!m.bullet().isEmpty()) x += m.level() * 8 + Math.max(8, theme.getStringWidth(m.bullet()) + 4);
        return x;
    }

    public static int indentRight(LineMarkup.Line m) {
        return (m.barRight() ? BAR_PAD : 0) + (m.code() || m.callout() != null ? BOX_PAD + 1 : 0);
    }

    /** Extra height above and below the text (boxes get room to breathe). */
    public static int padding(LineMarkup.Line m) {
        return m.code() || m.callout() != null ? BOX_PAD : 0;
    }

    public static boolean revealed(Info i) {
        return REVEALED.contains(key(i));
    }

    public static void reveal(Info i) {
        REVEALED.add(key(i));
    }

    private static String key(Info i) {
        return i.questId + ":" + i.line + ":" + i.markup.text().getString();
    }

    /** Before FTB draws the text: boxes, bars, bullets, dividers; returns true if the pose was pushed. */
    public static boolean before(TextField field, GuiGraphics g, Theme theme, int x, int y, int w, int h) {
        Info info = LINES.get(field);
        if (info == null || info.markup.isPlain()) return false;
        LineMarkup.Line m = info.markup;
        Widget parent = field.getParent();
        int px = parent == null ? x : x - field.getPosX(), pw = parent == null ? w : parent.width;
        RenderSystem.enableBlend();
        switch (m.kind()) {
            case DIVIDER -> {
                int t = m.thick() ? 2 : 1;
                g.fill(px, y + h / 2 - t / 2, px + pw, y + h / 2 - t / 2 + t, DIVIDER);
                return false;
            }
            case DOUBLE_DIVIDER -> {
                int t = m.thick() ? 2 : 1;
                g.fill(px, y + h / 2 - 2 - t + 1, px + pw, y + h / 2 - 1, DIVIDER);
                g.fill(px, y + h / 2 + 1, px + pw, y + h / 2 + 1 + t, DIVIDER);
                return false;
            }
            case GAP -> {
                return false;
            }
            default -> {
            }
        }
        int pad = padding(m);
        if (m.callout() != null) {
            int c = m.callout().color;
            g.fill(px, y, px + pw, y + h, (c & 0x00FFFFFF) | 0x30000000);
            g.fill(px, y, px + 2, y + h, c);
        }
        if (m.code()) {
            g.fill(x - BOX_PAD - 1, y, x + w + BOX_PAD + 1, y + h, CODE_BG);
        }
        int bar = m.thick() ? 3 : 2;
        if (m.barLeft()) g.fill(px, y, px + bar, y + h, BAR);
        if (m.barRight()) g.fill(px + pw - bar, y, px + pw, y + h, BAR);
        if (!m.bullet().isEmpty()) {
            int bw = theme.getStringWidth(m.bullet());
            int bx = x - bw - 3;
            theme.drawString(g, Component.literal(m.bullet()), bx, y + pad, Theme.SHADOW);
        }
        if (pad > 0) {
            g.pose().pushPose();
            g.pose().translate(0, pad, 0);
            return true;
        }
        return false;
    }

    /** After FTB drew the text: spoiler cover, and undo the padding translation. */
    public static void after(TextField field, GuiGraphics g, Theme theme, int x, int y, int w, int h, boolean pushed) {
        if (pushed) g.pose().popPose();
        Info info = LINES.get(field);
        if (info == null || !info.markup.spoiler() || revealed(info)) return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 10);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, SPOILER);
        Component hint = Component.translatable("quest_clusters_editor.spoiler");
        int hw = theme.getStringWidth(hint);
        if (hw < w) {
            theme.drawString(g, hint, x + (w - hw) / 2, y + (h - theme.getFontHeight()) / 2 + 1, dev.ftb.mods.ftblibrary.icon.Color4I.rgb(0x9A9A9A), 0);
        }
        g.pose().popPose();
    }
}
