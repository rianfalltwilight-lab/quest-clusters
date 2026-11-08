package com.questclusters.editor.client.gui;

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
    public static final ResourceLocation QE_TEXTURE = ResourceLocation.fromNamespaceAndPath("quest_enhance", "textures/gui/decorative_line.png");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("quest_clusters_editor", "textures/gui/decorative_line.png");
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
    /** For the dev self-test: segments drawn in the last frame. */
    public static int lastDrawn;

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
        if (m instanceof ChapterImage img && CanvasElement.parse(img.getClickAction().actionData()) instanceof CanvasElement.Anchor a) {
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
        Chapter chapter = qs.getSelectedChapter().orElse(null);
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        int drawn = 0;
        if (chapter == null || file == null || file.selfTeamData == null) {
            lastDrawn = 0;
            return;
        }
        List<List<String>> lines = ClientEditor.book().chapter(chapter.id).decorativeLines;
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
                quad(g, x1, y1, x2, y2, half);
                drawn++;
            }
        }
        lastDrawn = drawn;
    }

    private static void quad(GuiGraphics g, double x1, double y1, double x2, double y2, float half) {
        double len = Math.hypot(x2 - x1, y2 - y1);
        if (len < 0.5 || half <= 0) return;
        float nx = (float) (-(y2 - y1) / len * half), ny = (float) ((x2 - x1) / len * half);
        ResourceLocation tex = texture();
        int[] size = com.questclusters.client.Textures.size(tex);
        // an animated strip (with .mcmeta) shows its first frame; U repeats along the line like Quest Enhance's
        float v1 = size == null ? 1F : (float) size[2] / size[1];
        float u1 = (float) (len / half / 2D);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, tex);
        Matrix4f m = g.pose().last().pose();
        var buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buf.addVertex(m, (float) x1 + nx, (float) y1 + ny, 0).setUv(0, 0).setColor(START);
        buf.addVertex(m, (float) x2 + nx, (float) y2 + ny, 0).setUv(u1, 0).setColor(END);
        buf.addVertex(m, (float) x2 - nx, (float) y2 - ny, 0).setUv(u1, v1).setColor(END);
        buf.addVertex(m, (float) x1 - nx, (float) y1 - ny, 0).setUv(0, v1).setColor(START);
        BufferUploader.drawWithShader(buf.buildOrThrow());
    }
}
