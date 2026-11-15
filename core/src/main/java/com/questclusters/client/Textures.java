package com.questclusters.client;

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
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), path + ".png");
    }

    /**
     * Pixel size of a texture and the height of its first frame (animated textures are a vertical strip of square
     * frames with a .mcmeta file), read once from the resource manager; null if it can't be read.
     */
    public static int[] size(net.minecraft.resources.ResourceLocation rl) {
        return TEXTURE_SIZES.computeIfAbsent(rl, k -> {
            var rm = net.minecraft.client.Minecraft.getInstance().getResourceManager();
            try (var in = rm.open(k);
                 var img = com.mojang.blaze3d.platform.NativeImage.read(in)) {
                int w = img.getWidth(), h = img.getHeight();
                boolean animated = h > w && rm.getResource(k.withPath(k.getPath() + ".mcmeta")).isPresent();
                return new int[]{w, h, animated ? w : h};
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
    public record Animation(int frameWidth, int frameHeight, int columns, int[] frames, int[] ticks, int totalTicks) {
        /** Frame index shown at the given time (wall clock, so it keeps playing while the game is paused). */
        public int frameAt(long millis) {
            long t = (millis / 50L) % Math.max(1, totalTicks);
            for (int i = 0; i < frames.length; i++) {
                if (t < ticks[i]) return frames[i];
                t -= ticks[i];
            }
            return frames[frames.length - 1];
        }
    }

    private static final java.util.Map<net.minecraft.resources.ResourceLocation, java.util.Optional<Animation>> ANIMATIONS = new java.util.HashMap<>();

    public static Animation animation(net.minecraft.resources.ResourceLocation rl) {
        return ANIMATIONS.computeIfAbsent(rl, k -> java.util.Optional.ofNullable(readAnimation(k))).orElse(null);
    }

    private static Animation readAnimation(net.minecraft.resources.ResourceLocation rl) {
        int[] size = size(rl);
        var meta = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(rl.withPath(rl.getPath() + ".mcmeta"));
        if (size == null || meta.isEmpty()) {
            return null;
        }
        try (var reader = meta.get().openAsReader()) {
            var root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("animation")) {
                return null;
            }
            return parseAnimation(root.getAsJsonObject("animation"), size[0], size[1]);
        } catch (Exception e) {
            return null;
        }
    }

    /** The "animation" section for a texture of {@code w} x {@code h} px; null if it describes fewer than 2 frames. */
    public static Animation parseAnimation(com.google.gson.JsonObject a, int w, int h) {
        int defTime = a.has("frametime") ? Math.max(1, a.get("frametime").getAsInt()) : 1;
        int fw = a.has("width") ? a.get("width").getAsInt() : Math.min(w, h);
        int fh = a.has("height") ? a.get("height").getAsInt() : (a.has("width") ? fw : Math.min(w, h));
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
        return new Animation(fw, fh, columns, frames, ticks, java.util.Arrays.stream(ticks).sum());
    }

    public static void clearCache() {
        ANIMATIONS.clear();
        TEXTURE_SIZES.clear();
    }
}
