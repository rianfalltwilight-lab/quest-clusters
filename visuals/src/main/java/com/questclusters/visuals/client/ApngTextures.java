package com.questclusters.visuals.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.questclusters.visuals.QuestClustersVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Animated PNGs: any image whose name ends in {@code .apng.png} (the convention Certain Questing Additions used) is
 * decoded once, every frame composited to a full-size texture, and the current frame bound instead of the file.
 * Works wherever FTB Library draws an image icon (quest icons, description images, chapter images...).
 */
public final class ApngTextures {
    private static final Map<ResourceLocation, Loaded> CACHE = new HashMap<>();
    private static final long START = System.currentTimeMillis();

    private record Loaded(Apng.Animation anim, List<ResourceLocation> frames) {
    }

    private ApngTextures() {
    }

    public static boolean isApngName(ResourceLocation rl) {
        return rl.getPath().endsWith(".apng.png");
    }

    /** Binds the current frame if the texture is an animated PNG; false = let FTB Library bind it normally. */
    public static boolean bindIfAnimated(ResourceLocation rl) {
        if (rl == null || !isApngName(rl)) return false;
        Loaded l = CACHE.computeIfAbsent(rl, ApngTextures::load);
        if (l == null) return false;
        int i = l.anim().frameAt(System.currentTimeMillis() - START);
        RenderSystem.setShaderTexture(0, l.frames().get(i));
        return true;
    }

    /** Resource reload: drop decoded animations so changed packs are picked up. */
    public static void clear() {
        var tm = Minecraft.getInstance().getTextureManager();
        CACHE.values().forEach(l -> {
            if (l != null) l.frames().forEach(tm::release);
        });
        CACHE.clear();
    }

    private static Loaded load(ResourceLocation rl) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(rl)) {
            Apng.Animation a = Apng.parse(in.readAllBytes());
            if (a == null) return null; // plain PNG with an .apng.png name: draw it normally
            List<ResourceLocation> ids = new ArrayList<>();
            NativeImage canvas = new NativeImage(a.width(), a.height(), true);
            NativeImage previous = null;
            for (int n = 0; n < a.frames().size(); n++) {
                Apng.Frame f = a.frames().get(n);
                if (f.dispose() == Apng.DISPOSE_PREVIOUS) {
                    previous = copy(canvas);
                }
                try (NativeImage img = NativeImage.read(f.png())) {
                    for (int y = 0; y < f.height() && f.y() + y < a.height(); y++) {
                        for (int x = 0; x < f.width() && f.x() + x < a.width(); x++) {
                            int src = img.getPixelRGBA(x, y);
                            int cx = f.x() + x, cy = f.y() + y;
                            canvas.setPixelRGBA(cx, cy, f.blend() == Apng.BLEND_OVER ? over(src, canvas.getPixelRGBA(cx, cy)) : src);
                        }
                    }
                }
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(QuestClustersVisuals.MOD_ID,
                        "apng/" + Integer.toHexString(rl.hashCode()) + "/" + n);
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(copy(canvas)));
                ids.add(id);
                // dispose after showing this frame
                if (f.dispose() == Apng.DISPOSE_BACKGROUND) {
                    for (int y = f.y(); y < Math.min(a.height(), f.y() + f.height()); y++)
                        for (int x = f.x(); x < Math.min(a.width(), f.x() + f.width()); x++) canvas.setPixelRGBA(x, y, 0);
                } else if (f.dispose() == Apng.DISPOSE_PREVIOUS && previous != null) {
                    canvas.close();
                    canvas = previous;
                    previous = null;
                }
            }
            canvas.close();
            if (previous != null) previous.close();
            return new Loaded(a, ids);
        } catch (Exception e) {
            QuestClustersVisuals.LOGGER.warn("Could not load animated PNG {}: {}", rl, e.toString());
            return null;
        }
    }

    private static NativeImage copy(NativeImage src) {
        NativeImage n = new NativeImage(src.getWidth(), src.getHeight(), true);
        n.copyFrom(src);
        return n;
    }

    /** Source-over blend of two ABGR pixels (NativeImage's layout). */
    static int over(int src, int dst) {
        int sa = (src >>> 24) & 255;
        if (sa == 255) return src;
        if (sa == 0) return dst;
        int da = (dst >>> 24) & 255;
        float a = sa / 255F, b = da / 255F * (1 - a), oa = a + b;
        int r = Math.round(((src & 255) * a + (dst & 255) * b) / oa);
        int g = Math.round((((src >> 8) & 255) * a + ((dst >> 8) & 255) * b) / oa);
        int bl = Math.round((((src >> 16) & 255) * a + ((dst >> 16) & 255) * b) / oa);
        return (Math.round(oa * 255) << 24) | (bl << 16) | (g << 8) | r;
    }
}
