package com.questclusters.editor.client.gui;

import com.questclusters.platform.Rl;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.questclusters.api.ClusterCanvas;
import com.questclusters.editor.client.ClientEditor;
import com.questclusters.editor.data.CanvasElement;
import com.questclusters.editor.data.NodeKeys;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPositionableButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decorative lines: drawn from the first node of a line to each other node (spokes), behind quests and above
 * chapter images. A node hidden inside a collapsed cluster is drawn at the cluster box instead.
 * Sits in the panel's background layer (after the images), so it works whichever addon renders dependency lines.
 */
public final class DecorativeLinesWidget extends Widget {
    private static final int START = 0xFF00B4B4, END = 0xFF008787; // (0,180,180) fading to 3/4 brightness
    /**
     * The line texture, repeated along the line and tinted with the colours above. Resource packs made for Quest
     * Enhance replace {@link #QE_TEXTURE}; if none does, ours is used (and can be replaced the same way).
     */
    public static final ResourceLocation QE_TEXTURE = Rl.of("quest_enhance", "textures/gui/decorative_line.png");
    public static final ResourceLocation TEXTURE = Rl.of("quest_clusters_editor", "textures/gui/decorative_line.png");
    private static ResourceLocation texture;

    /** The texture in use; looked up again after a resource reload. */
    public static ResourceLocation texture() {
        if (texture == null) {
            texture = Minecraft.getInstance().getResourceManager().getResource(QE_TEXTURE).isPresent() ? QE_TEXTURE : TEXTURE;
        }
        return texture;
    }

    public static void resourcesReloaded() {
        texture = null;
    }
    private final QuestPanel questPanel;
    private final Map<String, Widget> nodes = new HashMap<>();
    /** For the dev self-test: segments drawn in the last frame, and the texture / frame / flow of the last one. */
    public static int lastDrawn;
    public static ResourceLocation lastTexture;
    public static float lastV0, lastFlow;

    public DecorativeLinesWidget(QuestPanel panel) {
        super(panel);
        this.questPanel = panel;
        setDrawLayer(DrawLayer.BACKGROUND);
    }

    public void align() {
        nodes.clear();
        for (Widget w : questPanel.getWidgets()) {
            if (w instanceof QuestPositionableButton pos) {
                String key = key(pos.moveAndDeleteFocus());
                if (key != null) nodes.put(key, w);
            }
        }
        setPosAndSize(0, 0, 1, 1);
    }

    static String key(Movable m) {
        if (m instanceof Quest q) return NodeKeys.quest(q.id);
        if (m instanceof QuestLink l) return NodeKeys.link(l.id);
        if (m instanceof ChapterImage img && CanvasElement.parse(com.questclusters.platform.Ftbq.clickData(img)) instanceof CanvasElement.Anchor a) {
            return a.nodeKey();
        }
        return null;
    }

    @Override
    public boolean collidesWith(int x, int y, int w, int h) {
        return true;
    }

    @Override
    public boolean checkMouseOver(int mouseX, int mouseY) {
        return false;
    }

    @Override
    public boolean shouldAddMouseOverText() {
        return false;
    }

    @Override
    public void addMouseOverText(TooltipList list) {
    }

    @Override
    public void draw(GuiGraphics g, Theme theme, int x, int y, int w, int h) {
        QuestScreen qs = (QuestScreen) getGui();
        Chapter chapter = ((com.questclusters.mixin.client.QuestScreenAccessor) qs).quest_clusters$getSelectedChapter();
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        int drawn = 0;
        if (chapter == null || file == null || file.selfTeamData == null) {
            lastDrawn = 0;
            return;
        }
        var book = ClientEditor.book();
        var extras = book.chapter(chapter.id);
        List<List<String>> lines = extras.decorativeLines;
        boolean edit = file.canEdit();
        float half = (float) (qs.getZoom() * 3D / 16D);
        for (List<String> line : lines) {
            Widget[] ends = new Widget[line.size()];
            boolean[] anchor = new boolean[line.size()];
            int resolved = 0;
            boolean hide = false;
            for (int i = 0; i < line.size(); i++) {
                Widget wd = nodes.get(line.get(i));
                if (wd == null) continue;
                Movable m = ((QuestPositionableButton) wd).moveAndDeleteFocus();
                anchor[i] = m instanceof ChapterImage;
                if (!edit && m instanceof QuestObject qo && !qo.isVisible(file.selfTeamData)) {
                    hide = true; // a line to something the player cannot see would give it away
                    break;
                }
                if (!wd.shouldDraw()) {
                    wd = ClusterCanvas.standIn(questPanel, m);
                    anchor[i] = false;
                    if (wd == null) continue;
                }
                ends[i] = wd;
                resolved++;
            }
            if (hide || resolved < 2) continue;
            int first = -1;
            for (int i = 0; i < ends.length; i++) {
                if (ends[i] != null) {
                    first = i;
                    break;
                }
            }
            Widget c = ends[first];
            // the line's look comes from the node it starts at (its own preset, else the chapter's default)
            var preset = book.preset(extras.presetFor(line.get(first)));
            for (int i = first + 1; i < ends.length; i++) {
                Widget o = ends[i];
                if (o == null || o == c) continue; // both inside the same collapsed cluster
                double x1 = c.getX() + c.width / 2D, y1 = c.getY() + c.height / 2D;
                double x2 = o.getX() + o.width / 2D, y2 = o.getY() + o.height / 2D;
                double len = Math.hypot(x2 - x1, y2 - y1);
                if (len < 1) continue;
                double ux = (x2 - x1) / len, uy = (y2 - y1) / len;
                if (anchor[first]) {
                    double cut = Math.min(c.width, c.height) / 2D;
                    x1 += ux * cut;
                    y1 += uy * cut;
                }
                if (anchor[i]) {
                    double cut = Math.min(o.width, o.height) / 2D;
                    x2 -= ux * cut;
                    y2 -= uy * cut;
                }
                quad(g, x1, y1, x2, y2, half, preset);
                drawn++;
            }
        }
        lastDrawn = drawn;
    }

    /** A preset draws its own image in its own colours; without one, the built-in texture tinted teal. */
    private static void quad(GuiGraphics g, double x1, double y1, double x2, double y2, float half,
                             com.questclusters.editor.data.LinePreset preset) {
        double len = Math.hypot(x2 - x1, y2 - y1);
        if (preset != null) half *= preset.width();
        if (len < 0.5 || half <= 0) return;
        float nx = (float) (-(y2 - y1) / len * half), ny = (float) ((x2 - x1) / len * half);
        ResourceLocation presetTex = preset == null ? null : com.questclusters.client.Textures.file(preset.texture());
        ResourceLocation tex = presetTex != null ? presetTex : texture();
        int start = presetTex != null ? preset.color() : START, end = presetTex != null ? preset.color() : END;
        long now = System.currentTimeMillis();
        com.questclusters.client.Textures.Frame frame = null;
        int[] size = null;
        if (tex.getPath().endsWith(".gif")) {
            // GIF: its current frame is uploaded into one texture
            tex = com.questclusters.editor.client.GifTextures.currentFrame(tex);
            if (tex == null) return;
        } else {
            size = com.questclusters.client.Textures.size(tex);
            frame = size == null ? null : com.questclusters.client.Textures.frame(tex, now);
        }
        // a preset may make the pattern flow (repeats per second)
        float flow = preset == null || preset.scroll() == 0F ? 0F
                : (float) (((now % 1_000_000L) / 1000D * preset.scroll()) % 1D);
        lastTexture = tex;
        lastV0 = frame == null ? 0F : frame.v0();
        lastFlow = flow;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, tex);
        Matrix4f m = g.pose().last().pose();
        if (frame == null) {
            // a still texture repeats along the line (texture wrap), like Quest Enhance's
            float u0 = -flow, u1 = u0 + (float) (len / half / 2D);
            com.questclusters.platform.Quads.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
                    .vertex(m, (float) x1 + nx, (float) y1 + ny, 0).uv(u0, 0F).color(start).next()
                    .vertex(m, (float) x2 + nx, (float) y2 + ny, 0).uv(u1, 0F).color(end).next()
                    .vertex(m, (float) x2 - nx, (float) y2 - ny, 0).uv(u1, 1F).color(end).next()
                    .vertex(m, (float) x1 - nx, (float) y1 - ny, 0).uv(u0, 1F).color(start).next()
                    .draw();
            return;
        }
        // an animated texture: one quad per repeat, each showing exactly the current frame (texture wrap would run
        // into the neighbouring frames of strips and grids), tiles keep the frame's proportions
        float framePxW = (frame.u1() - frame.u0()) * size[0], framePxH = (frame.v1() - frame.v0()) * size[1];
        double tile = 2D * half * framePxW / Math.max(1F, framePxH);
        tiles(m, x1, y1, x2, y2, nx, ny, len, tile, flow, frame, start, end, 1F);
        if (frame.next() != null && frame.blend() > 0F) {
            tiles(m, x1, y1, x2, y2, nx, ny, len, tile, flow, frame.next(), start, end, frame.blend());
        }
    }

    private static void tiles(Matrix4f m, double x1, double y1, double x2, double y2, float nx, float ny, double len, double tile,
                              float flow, com.questclusters.client.Textures.Frame f, int start, int end, float alpha) {
        if (tile < 0.5) return;
        double dx = (x2 - x1) / len, dy = (y2 - y1) / len;
        var q = com.questclusters.platform.Quads.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (double t = -flow * tile; t < len; t += tile) {
            double a = Math.max(0D, t), b = Math.min(len, t + tile);
            if (b <= a) continue;
            float ua = f.u((float) ((a - t) / tile)), ub = f.u((float) ((b - t) / tile));
            int ca = fade(lerp(start, end, (float) (a / len)), alpha), cb = fade(lerp(start, end, (float) (b / len)), alpha);
            float ax = (float) (x1 + dx * a), ay = (float) (y1 + dy * a), bx = (float) (x1 + dx * b), by = (float) (y1 + dy * b);
            q.vertex(m, ax + nx, ay + ny, 0).uv(ua, f.v0()).color(ca).next()
                    .vertex(m, bx + nx, by + ny, 0).uv(ub, f.v0()).color(cb).next()
                    .vertex(m, bx - nx, by - ny, 0).uv(ub, f.v1()).color(cb).next()
                    .vertex(m, ax - nx, ay - ny, 0).uv(ua, f.v1()).color(ca).next();
        }
        q.draw();
    }

    private static int lerp(int a, int b, float t) {
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = a >>> shift & 0xFF, cb = b >>> shift & 0xFF;
            out |= Math.round(ca + (cb - ca) * t) << shift;
        }
        return out;
    }

    private static int fade(int argb, float alpha) {
        return Math.round((argb >>> 24) * alpha) << 24 | argb & 0xFFFFFF;
    }
}
