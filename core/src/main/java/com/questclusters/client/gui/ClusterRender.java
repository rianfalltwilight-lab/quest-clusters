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

    /**
     * Fills (x, y, w, h) with a texture: stretched, tiled, or nine-sliced with a border of {@code border} texture px.
     * Texture pixels map to screen pixels at {@code scale} (tile size and nine-slice borders follow the quest zoom).
     */
    static void textureFill(GuiGraphics g, String texture, int mode, int border, int x, int y, int w, int h,
                            float scale, float alpha) {
        net.minecraft.resources.ResourceLocation rl = com.questclusters.client.Textures.file(texture);
        if (rl == null || w <= 0 || h <= 0) {
            return;
        }
        int[] size = com.questclusters.client.Textures.size(rl);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
        try {
            if (size == null || mode == com.questclusters.data.ClusterStyle.MODE_STRETCH) {
                int tw = size == null ? 16 : size[0], th = size == null ? 16 : size[1], fh = size == null ? 16 : size[2];
                g.blit(rl, x, y, w, h, 0, 0, tw, fh, tw, th);
                return;
            }
            int tw = size[0], th = size[1], fh = size[2];
            if (mode == com.questclusters.data.ClusterStyle.MODE_TILE) {
                int sw = Math.max(1, Math.round(tw * scale)), sh = Math.max(1, Math.round(fh * scale));
                for (int ty = 0; ty < h; ty += sh) {
                    for (int tx = 0; tx < w; tx += sw) {
                        int dw = Math.min(sw, w - tx), dh = Math.min(sh, h - ty);
                        g.blit(rl, x + tx, y + ty, dw, dh, 0, 0, Math.max(1, Math.round(dw / scale)), Math.max(1, Math.round(dh / scale)), tw, th);
                    }
                }
                return;
            }
            // nine-slice
            int b = Math.min(border, Math.min(tw, fh) / 2);
            int sb = Math.min(Math.round(b * scale), Math.min(w, h) / 2);
            int[] dx = {x, x + sb, x + w - sb}, dw = {sb, w - 2 * sb, sb};
            int[] dy = {y, y + sb, y + h - sb}, dh = {sb, h - 2 * sb, sb};
            int[] u = {0, b, tw - b}, uw = {b, tw - 2 * b, b};
            int[] v = {0, b, fh - b}, vh = {b, fh - 2 * b, b};
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (dw[c] > 0 && dh[r] > 0 && uw[c] > 0 && vh[r] > 0) {
                        g.blit(rl, dx[c], dy[r], dw[c], dh[r], u[c], v[r], uw[c], vh[r], tw, th);
                    }
                }
            }
        } finally {
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }
    }
}
