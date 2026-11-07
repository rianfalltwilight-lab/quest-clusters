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
        if (tw == 0 || maxWidth <= 2) {
            return true;
        }
        float scale = Math.min(lineHeight / font.lineHeight, maxWidth / tw);
        net.minecraft.util.FormattedCharSequence line = text.getVisualOrderText();
        float minScale = Math.min(0.5F, lineHeight / font.lineHeight);
        if (scale < minScale) {
            // too long to fit at a readable size: keep the size, cut the text with an ellipsis
            scale = minScale;
            int avail = (int) (maxWidth / scale) - font.width("…");
            if (avail <= 0) {
                return false;
            }
            String cut = font.plainSubstrByWidth(text.getString(), avail) + "…";
            line = Component.literal(cut).withStyle(text.getStyle()).getVisualOrderText();
            tw = font.width(line);
        }
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 0);
        ps.scale(scale, scale, 1F);
        g.drawString(font, line, -tw / 2, -font.lineHeight / 2 + 1, color, false);
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

    private static final net.minecraft.resources.ResourceLocation DEFAULT_LINE_TEXTURE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ftbquests", "textures/gui/dependency.png");

    /**
     * Dependency line drawn like FTB Quests' own: the chapter theme's line texture scrolling along the line,
     * fading from a darker start colour to the end colour.
     *
     * @param half      half the line thickness in px
     * @param scroll    texture offset (0..1), advanced over time for the flowing effect
     */
    static void texturedLine(GuiGraphics g, double x1, double y1, double x2, double y2, float half,
                             int startColor, int endColor, float scroll,
                             dev.ftb.mods.ftblibrary.icon.Icon themeTexture) {
        double len = Math.hypot(x2 - x1, y2 - y1);
        if (len < 1 || half <= 0) {
            return;
        }
        if (themeTexture instanceof dev.ftb.mods.ftblibrary.icon.ImageIcon img) {
            img.bindTexture();
        } else {
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, DEFAULT_LINE_TEXTURE);
        }
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x1, y1, 0);
        ps.mulPose(Axis.ZP.rotation((float) Math.atan2(y2 - y1, x2 - x1)));
        org.joml.Matrix4f m = ps.last().pose();
        float l = (float) len;
        float repeats = l / (half * 2F); // texture is square: one tile per line-width
        var buf = com.mojang.blaze3d.vertex.Tesselator.getInstance().begin(
                com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR);
        buf.addVertex(m, 0, -half, 0).setUv(repeats + scroll, 0).setColor(startColor);
        buf.addVertex(m, 0, half, 0).setUv(repeats + scroll, 1).setColor(startColor);
        buf.addVertex(m, l, half, 0).setUv(scroll, 1).setColor(endColor);
        buf.addVertex(m, l, -half, 0).setUv(scroll, 0).setColor(endColor);
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buf.buildOrThrow());
        ps.popPose();
    }

    /** The darker start colour FTB Quests uses at the dependent end of a line. */
    static int darker(int argb) {
        return FastColor.ARGB32.color(FastColor.ARGB32.alpha(argb), FastColor.ARGB32.red(argb) * 3 / 4,
                FastColor.ARGB32.green(argb) * 3 / 4, FastColor.ARGB32.blue(argb) * 3 / 4);
    }
}
