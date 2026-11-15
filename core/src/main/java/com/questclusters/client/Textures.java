package com.questclusters.client;

import com.questclusters.platform.Rl;

/** Image references as stored in quest books and our data, resolved to texture files (client only). */
public final class Textures {
    private Textures() {
    }

    private static final java.util.Map<net.minecraft.resources.ResourceLocation, int[]> TEXTURE_SIZES = new java.util.HashMap<>();

    /**
     * The texture file behind a stored image reference. FTB Library's image picker returns textures that live in the
     * block atlas by their short sprite id ({@code minecraft:block/stone}, i.e. {@code textures/block/stone.png});
     * binding that id as a file gives the purple-black missing texture. Full paths are returned unchanged.
     */
    public static net.minecraft.resources.ResourceLocation file(String texture) {
        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(texture);
        if (rl == null || rl.getPath().endsWith(".png") || rl.getPath().endsWith(".gif")) {
            return rl;
        }
        String path = rl.getPath().startsWith("textures/") ? rl.getPath() : "textures/" + rl.getPath();
        return Rl.of(rl.getNamespace(), path + ".png");
    }

    /**
     * Pixel size of a texture and the height of one frame (a texture with an animated .mcmeta: its frame height,
     * whatever the layout), read once from the resource manager; null if it can't be read.
     */
    public static int[] size(net.minecraft.resources.ResourceLocation rl) {
        return TEXTURE_SIZES.computeIfAbsent(rl, k -> {
            var rm = net.minecraft.client.Minecraft.getInstance().getResourceManager();
            try (var in = rm.open(k);
                 var img = com.mojang.blaze3d.platform.NativeImage.read(in)) {
                int w = img.getWidth(), h = img.getHeight();
                Animation a = readAnimation(k, w, h);
                return new int[]{w, h, a != null ? a.frameHeight() : h};
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Frame animation of a texture from its .mcmeta ("animation": frametime, frames as indices or {index, time},
     * optional frame width / height; frames are stacked vertically). Null if the texture is not animated.
     *
     * @param frameWidth  width of one frame in texture px
     * @param frameHeight height of one frame in texture px
     * @param columns     frames per row (vertical strips have 1)
     * @param frames      frame indices in play order
     * @param ticks       how long each entry of {@code frames} shows, in game ticks (1/20 s)
     */
    public record Animation(int frameWidth, int frameHeight, int columns, int[] frames, int[] ticks, int totalTicks,
                            boolean interpolate) {
        public Animation(int frameWidth, int frameHeight, int columns, int[] frames, int[] ticks, int totalTicks) {
            this(frameWidth, frameHeight, columns, frames, ticks, totalTicks, false);
        }

        /** Frame index shown at the given time (wall clock, so it keeps playing while the game is paused). */
        public int frameAt(long millis) {
            return frames[entryAt(millis)];
        }

        /** Index into {@code frames} at the given time. */
        public int entryAt(long millis) {
            long t = (millis / 50L) % Math.max(1, totalTicks);
            for (int i = 0; i < frames.length; i++) {
                if (t < ticks[i]) return i;
                t -= ticks[i];
            }
            return frames.length - 1;
        }

        /** How far into the current entry (0..1), for {@code interpolate}. */
        public float progress(long millis) {
            double t = (millis / 50D) % Math.max(1, totalTicks);
            for (int tick : ticks) {
                if (t < tick) return (float) (t / tick);
                t -= tick;
            }
            return 0F;
        }
    }

    /**
     * The part of a texture to draw now, in 0..1 texture coordinates: the current frame of an animated texture (any
     * layout: vertical or horizontal strips and grids, square or not), and with {@code interpolate} the next frame and
     * how much of it to blend over the first ({@code next} null otherwise).
     */
    public record Frame(float u0, float v0, float u1, float v1, Frame next, float blend) {
        /** Maps a position (0..1) within the frame to the texture. */
        public float u(float t) {
            return u0 + (u1 - u0) * t;
        }

        public float v(float t) {
            return v0 + (v1 - v0) * t;
        }
    }

    /** The current frame of the texture, or null if it is not animated (draw the whole texture). */
    public static Frame frame(net.minecraft.resources.ResourceLocation rl, long millis) {
        Animation a = animation(rl);
        int[] size = a == null ? null : size(rl);
        if (size == null) return null;
        int entry = a.entryAt(millis);
        Frame next = null;
        float blend = 0F;
        if (a.interpolate()) {
            blend = a.progress(millis);
            next = rect(a, a.frames()[(entry + 1) % a.frames().length], size, null, 0F);
        }
        return rect(a, a.frames()[entry], size, next, blend);
    }

    private static Frame rect(Animation a, int index, int[] size, Frame next, float blend) {
        int col = index % a.columns(), row = index / a.columns();
        float u0 = (float) col * a.frameWidth() / size[0], v0 = (float) row * a.frameHeight() / size[1];
        return new Frame(u0, v0, u0 + (float) a.frameWidth() / size[0], v0 + (float) a.frameHeight() / size[1], next, blend);
    }

    private static final java.util.Map<net.minecraft.resources.ResourceLocation, java.util.Optional<Animation>> ANIMATIONS = new java.util.HashMap<>();

    public static Animation animation(net.minecraft.resources.ResourceLocation rl) {
        return ANIMATIONS.computeIfAbsent(rl, k -> java.util.Optional.ofNullable(readAnimation(k))).orElse(null);
    }

    private static Animation readAnimation(net.minecraft.resources.ResourceLocation rl) {
        int[] size = size(rl);
        return size == null ? null : readAnimation(rl, size[0], size[1]);
    }

    private static Animation readAnimation(net.minecraft.resources.ResourceLocation rl, int w, int h) {
        var meta = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(rl.withPath(rl.getPath() + ".mcmeta"));
        if (meta.isEmpty()) {
            return null;
        }
        try (var reader = meta.get().openAsReader()) {
            var root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("animation")) {
                return null;
            }
            return parseAnimation(root.getAsJsonObject("animation"), w, h);
        } catch (Exception e) {
            return null;
        }
    }

    /** The "animation" section for a texture of {@code w} x {@code h} px; null if it describes fewer than 2 frames. */
    public static Animation parseAnimation(com.google.gson.JsonObject a, int w, int h) {
        int defTime = a.has("frametime") ? Math.max(1, a.get("frametime").getAsInt()) : 1;
        // as vanilla: neither given -> square frames of the shorter side; one given -> the other is the full image
        int fw, fh;
        if (a.has("width")) {
            fw = a.get("width").getAsInt();
            fh = a.has("height") ? a.get("height").getAsInt() : h;
        } else if (a.has("height")) {
            fw = w;
            fh = a.get("height").getAsInt();
        } else {
            fw = fh = Math.min(w, h);
        }
        if (fw <= 0 || fh <= 0 || fw > w || fh > h) {
            return null;
        }
        int columns = Math.max(1, w / fw), count = columns * Math.max(1, h / fh);
        java.util.List<int[]> list = new java.util.ArrayList<>();
        if (a.has("frames")) {
            for (var e : a.getAsJsonArray("frames")) {
                if (e.isJsonPrimitive()) {
                    list.add(new int[]{e.getAsInt(), defTime});
                } else {
                    var o = e.getAsJsonObject();
                    list.add(new int[]{o.get("index").getAsInt(), o.has("time") ? Math.max(1, o.get("time").getAsInt()) : defTime});
                }
            }
        } else {
            for (int i = 0; i < count; i++) {
                list.add(new int[]{i, defTime});
            }
        }
        list.removeIf(f -> f[0] < 0 || f[0] >= count);
        if (list.size() < 2) {
            return null;
        }
        int[] frames = list.stream().mapToInt(f -> f[0]).toArray(), ticks = list.stream().mapToInt(f -> f[1]).toArray();
        boolean interpolate = a.has("interpolate") && a.get("interpolate").getAsBoolean();
        return new Animation(fw, fh, columns, frames, ticks, java.util.Arrays.stream(ticks).sum(), interpolate);
    }

    /**
     * What is wrong with a texture's frame animation, as a translation key (quest_clusters.animation.*), or null if it
     * has none or it is fine: frames larger than the image, an image size that is not a whole number of frames,
     * a single frame, or a .mcmeta file that cannot be read.
     */
    public static String animationProblem(net.minecraft.resources.ResourceLocation rl) {
        var meta = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(rl.withPath(rl.getPath() + ".mcmeta"));
        int[] size = size(rl);
        if (meta.isEmpty() || size == null) return null;
        try (var reader = meta.get().openAsReader()) {
            var root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("animation")) return null;
            var a = root.getAsJsonObject("animation");
            int w = size[0], h = size[1];
            int fw = a.has("width") ? a.get("width").getAsInt() : a.has("height") ? w : Math.min(w, h);
            int fh = a.has("height") ? a.get("height").getAsInt() : a.has("width") ? h : Math.min(w, h);
            if (fw <= 0 || fh <= 0 || fw > w || fh > h) return "quest_clusters.animation.too_big";
            if (w % fw != 0 || h % fh != 0) return "quest_clusters.animation.uneven";
            if (parseAnimation(a, w, h) == null) return "quest_clusters.animation.one_frame";
            return null;
        } catch (Exception e) {
            return "quest_clusters.animation.bad_file";
        }
    }

    public static void clearCache() {
        ANIMATIONS.clear();
        TEXTURE_SIZES.clear();
    }
}
