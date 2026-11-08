package com.questclusters.editor.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.questclusters.editor.data.CanvasElement;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.QuestShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/** Draws special canvas elements in place of the chapter image (text, guide point, GIF/video placeholders). */
public final class CanvasRender {
    private CanvasRender() {
    }

    /** @return true if the element was drawn (FTB Quests must then skip its own image draw) */
    public static boolean draw(GuiGraphics g, Theme theme, ChapterImage img, CanvasElement el, boolean selected,
                               int x, int y, int w, int h) {
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        boolean transparent = file != null && file.selfTeamData != null && !img.shouldShowImage(file.selfTeamData);
        if (el instanceof CanvasElement.Anchor) {
            drawAnchor(g, x, y, w, h, selected);
            return true;
        }
        PoseStack ps = g.pose();
        float ox = img.isAlignToCorner() ? 0 : w / 2F, oy = img.isAlignToCorner() ? 0 : h / 2F;
        ps.pushPose();
        ps.translate(x + ox, y + oy, 0);
        ps.mulPose(Axis.ZP.rotationDegrees((float) img.getRotation()));
        ps.translate(-ox, -oy, 0);
        int alpha = transparent ? 100 : img.getAlpha();
        int rgb = img.getColor().rgba() & 0xFFFFFF;
        switch (el) {
            case CanvasElement.Text t -> drawText(g, theme, t, w, h, img.isAlignToCorner(), (alpha << 24) | rgb);
            case CanvasElement.Gif gif -> {
                ResourceLocation rl = ResourceLocation.tryParse(gif.resource());
                if (rl == null || !com.questclusters.editor.client.GifTextures.draw(g, rl, 0, 0, w, h, (alpha << 24) | rgb)) {
                    g.fill(0, 0, w, h, (alpha << 24) | 0x212121);
                }
            }
            case CanvasElement.Video v -> {
                if (!img.getImage().isEmpty()) {
                    img.getImage().withColor(img.getColor().withAlpha(alpha)).draw(g, 0, 0, w, h);
                } else {
                    g.fill(0, 0, w, h, (alpha << 24) | 0x212121);
                }
                Font font = Minecraft.getInstance().font;
                float s = Math.max(0.5F, Math.min(w, h) / 24F);
                ps.pushPose();
                ps.translate(w / 2F, h / 2F, 0);
                ps.scale(s, s, 1);
                g.drawString(font, "▶", -font.width("▶") / 2, -font.lineHeight / 2, 0xFFFFFFFF, true);
                ps.popPose();
            }
            default -> {
            }
        }
        if (selected) {
            int a = (int) (45 + 20 * Math.sin(System.currentTimeMillis() * 0.003D));
            g.fill(0, 0, w, h, (a << 24) | 0xFFFFFF);
        }
        ps.popPose();
        return true;
    }

    static void drawAnchor(GuiGraphics g, int x, int y, int w, int h, boolean selected) {
        // the layers are translucent; whatever was drawn before (text flushes) may have left blending off
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        QuestShape shape = QuestShape.get("circle");
        shape.getShape().withColor(Color4I.DARK_GRAY).draw(g, x, y, w, h);
        shape.getBackground().withColor(Color4I.WHITE.withAlpha(150)).draw(g, x, y, w, h);
        shape.getOutline().withColor(Color4I.rgb(0x808080)).draw(g, x, y, w, h);
        if (selected) {
            int a = (int) (128 + 50 * Math.sin(System.currentTimeMillis() * 0.003D));
            shape.getBackground().withColor(Color4I.WHITE.withAlpha(a)).draw(g, x, y, w, h);
        }
    }

    private static void drawText(GuiGraphics g, Theme theme, CanvasElement.Text t, int w, int h, boolean corner, int argb) {
        ResourceLocation font = ResourceLocation.tryParse(t.font());
        Component c = Component.literal(t.text()).withStyle(font == null ? Style.EMPTY : Style.EMPTY.withFont(font));
        float tw = Math.max(1, theme.getStringWidth(c)), th = Math.max(1, theme.getFontHeight());
        float fit = Math.min(w / tw, h / th);
        float scale = (float) (Math.max(0.001F, fit) * Math.max(0.05, Math.min(10, t.scale())));
        PoseStack ps = g.pose();
        ps.pushPose();
        if (corner) {
            ps.scale(scale, scale, 1);
        } else {
            ps.translate(w / 2F, h / 2F, 0);
            ps.scale(scale, scale, 1);
            ps.translate(-tw / 2F, -th / 2F, 0);
        }
        theme.drawString(g, c, 0, 0, Color4I.rgba(argb), Theme.SHADOW);
        ps.popPose();
    }
}
