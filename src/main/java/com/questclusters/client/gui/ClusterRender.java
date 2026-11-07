package com.questclusters.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;

/** Small drawing helpers shared by the cluster widgets. */
final class ClusterRender {
    private ClusterRender() {
    }

    static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    static int lighten(int argb, float f) {
        int r = FastColor.ARGB32.red(argb), g = FastColor.ARGB32.green(argb), b = FastColor.ARGB32.blue(argb);
        r = (int) (r + (255 - r) * f);
        g = (int) (g + (255 - g) * f);
        b = (int) (b + (255 - b) * f);
        return FastColor.ARGB32.color(FastColor.ARGB32.alpha(argb), r, g, b);
    }

    static void outline(GuiGraphics g, int x, int y, int w, int h, int color, int t) {
        g.fill(x, y, x + w, y + t, color);
        g.fill(x, y + h - t, x + w, y + h, color);
        g.fill(x, y + t, x + t, y + h - t, color);
        g.fill(x + w - t, y + t, x + w, y + h - t, color);
    }

    /**
     * Draws text centred at (cx, cy), scaled so its line height is about {@code lineHeight} px and its width fits
     * {@code maxWidth}. Returns false if it would be too small to read.
     */
    static boolean centredText(GuiGraphics g, Component text, float cx, float cy, float lineHeight, float maxWidth, int color) {
        Font font = Minecraft.getInstance().font;
        int tw = font.width(text);
        if (tw == 0) {
            return true;
        }
        float scale = Math.min(lineHeight / font.lineHeight, maxWidth / tw);
        if (scale < 0.25F) {
            return false;
        }
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 0);
        ps.scale(scale, scale, 1F);
        g.drawString(font, text, -tw / 2, -font.lineHeight / 2 + 1, color, false);
        ps.popPose();
        return true;
    }

    /** Straight line from (x1,y1) to (x2,y2), thickness t px. */
    static void line(GuiGraphics g, double x1, double y1, double x2, double y2, float t, int color) {
        double len = Math.hypot(x2 - x1, y2 - y1);
        if (len < 1) {
            return;
        }
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x1, y1, 0);
        ps.mulPose(Axis.ZP.rotation((float) Math.atan2(y2 - y1, x2 - x1)));
        ps.scale(1F, Math.max(t, 0.5F), 1F);
        g.fill(0, -1, (int) Math.round(len), 1, color);
        ps.popPose();
    }
}
