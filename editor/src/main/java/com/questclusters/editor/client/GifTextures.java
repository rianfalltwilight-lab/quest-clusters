package com.questclusters.editor.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.questclusters.editor.QuestClustersEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Animated GIFs from resource packs, decoded once, played by wall clock into one shared texture per GIF. */
public final class GifTextures {
    private static final class Anim {
        final ResourceLocation id;
        final DynamicTexture texture;
        final int width, height;
        final List<int[]> frames = new ArrayList<>(); // ARGB
        final List<Integer> delays = new ArrayList<>(); // ms
        long total;
        int shown = -1;

        Anim(ResourceLocation id, DynamicTexture texture, int width, int height) {
            this.id = id;
            this.texture = texture;
            this.width = width;
            this.height = height;
        }
    }

    private static final Map<ResourceLocation, Anim> CACHE = new HashMap<>();
    private static final Set<ResourceLocation> FAILED = new HashSet<>();

    private GifTextures() {
    }

    /** All GIFs under textures/ in the loaded resource packs, sorted. */
    public static List<ResourceLocation> list() {
        List<ResourceLocation> out = new ArrayList<>(Minecraft.getInstance().getResourceManager()
                .listResources("textures", rl -> rl.getPath().toLowerCase(Locale.ROOT).endsWith(".gif")).keySet());
        out.sort(java.util.Comparator.comparing(ResourceLocation::toString));
        return out;
    }

    public static float aspect(ResourceLocation gif) {
        Anim a = get(gif);
        return a == null || a.height == 0 ? 1F : a.width / (float) a.height;
    }

    /** Draws the current frame tinted with argb; false if the GIF cannot be decoded. */
    public static boolean draw(GuiGraphics g, ResourceLocation gif, int x, int y, int w, int h, int argb) {
        Anim a = get(gif);
        if (a == null || a.frames.isEmpty()) return false;
        int idx = 0;
        if (a.frames.size() > 1 && a.total > 0) {
            long t = System.currentTimeMillis() % a.total;
            while (idx < a.delays.size() - 1 && t >= a.delays.get(idx)) {
                t -= a.delays.get(idx);
                idx++;
            }
        }
        if (idx != a.shown) {
            a.shown = idx;
            NativeImage img = a.texture.getPixels();
            int[] px = a.frames.get(idx);
            for (int py = 0; py < a.height; py++) {
                for (int pxi = 0; pxi < a.width; pxi++) {
                    int c = px[py * a.width + pxi]; // ARGB -> ABGR
                    img.setPixelRGBA(pxi, py, (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16));
                }
            }
            a.texture.upload();
        }
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(((argb >> 16) & 0xFF) / 255F, ((argb >> 8) & 0xFF) / 255F, (argb & 0xFF) / 255F, (argb >>> 24) / 255F);
        g.blit(a.id, x, y, w, h, 0, 0, a.width, a.height, a.width, a.height);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        return true;
    }

    private static Anim get(ResourceLocation gif) {
        Anim a = CACHE.get(gif);
        if (a != null || FAILED.contains(gif)) return a;
        try {
            a = decode(gif);
            CACHE.put(gif, a);
        } catch (Exception e) {
            QuestClustersEditor.LOGGER.warn("Could not decode GIF {}: {}", gif, e.toString());
            FAILED.add(gif);
        }
        return a;
    }

    private static Anim decode(ResourceLocation gif) throws Exception {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(gif);
             ImageInputStream iis = ImageIO.createImageInputStream(in)) {
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            reader.setInput(iis, false);
            int n = reader.getNumImages(true);
            int sw = 0, sh = 0;
            IIOMetadata sm = reader.getStreamMetadata();
            if (sm != null) {
                Node lsd = child(sm.getAsTree("javax_imageio_gif_stream_1.0"), "LogicalScreenDescriptor");
                if (lsd != null) {
                    sw = attr(lsd, "logicalScreenWidth");
                    sh = attr(lsd, "logicalScreenHeight");
                }
            }
            BufferedImage first = reader.read(0);
            if (sw <= 0 || sh <= 0) {
                sw = first.getWidth();
                sh = first.getHeight();
            }
            if (sw > 2048 || sh > 2048 || n > 1000) throw new IllegalArgumentException("GIF too large");
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(QuestClustersEditor.MOD_ID,
                    "dynamic_gif/" + UUID.nameUUIDFromBytes(gif.toString().getBytes()));
            DynamicTexture tex = new DynamicTexture(sw, sh, true);
            Minecraft.getInstance().getTextureManager().register(id, tex);
            Anim a = new Anim(id, tex, sw, sh);

            BufferedImage canvas = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
            BufferedImage previous = null;
            int[] prevRect = null;
            String prevDisposal = "none";
            for (int i = 0; i < n; i++) {
                // dispose of the previous frame first
                if ("restoreToBackgroundColor".equals(prevDisposal) && prevRect != null) {
                    Graphics2D g = canvas.createGraphics();
                    g.setComposite(java.awt.AlphaComposite.Clear);
                    g.fillRect(prevRect[0], prevRect[1], prevRect[2], prevRect[3]);
                    g.dispose();
                } else if ("restoreToPrevious".equals(prevDisposal) && previous != null) {
                    canvas = copy(previous);
                }
                BufferedImage frame = i == 0 ? first : reader.read(i);
                Node meta = reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                Node desc = child(meta, "ImageDescriptor"), gce = child(meta, "GraphicControlExtension");
                int fx = desc == null ? 0 : attr(desc, "imageLeftPosition"), fy = desc == null ? 0 : attr(desc, "imageTopPosition");
                String disposal = gce == null ? "none" : gce.getAttributes().getNamedItem("disposalMethod").getNodeValue();
                int delay = gce == null ? 10 : attr(gce, "delayTime");
                previous = "restoreToPrevious".equals(disposal) ? copy(canvas) : null;
                Graphics2D g = canvas.createGraphics();
                g.drawImage(frame, fx, fy, null);
                g.dispose();
                a.frames.add(canvas.getRGB(0, 0, sw, sh, null, 0, sw));
                int ms = Math.max(20, delay * 10);
                a.delays.add(ms);
                a.total += ms;
                prevRect = new int[]{fx, fy, frame.getWidth(), frame.getHeight()};
                prevDisposal = disposal;
            }
            reader.dispose();
            return a;
        }
    }

    private static BufferedImage copy(BufferedImage src) {
        BufferedImage c = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = c.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return c;
    }

    private static Node child(Node n, String name) {
        for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (name.equals(c.getNodeName())) return c;
        }
        return null;
    }

    private static int attr(Node n, String name) {
        Node a = n.getAttributes().getNamedItem(name);
        try {
            return a == null ? 0 : Integer.parseInt(a.getNodeValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
