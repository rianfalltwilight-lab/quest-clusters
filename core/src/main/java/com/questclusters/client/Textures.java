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
        if (rl == null || rl.getPath().endsWith(".png")) {
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

    public static void clearCache() {
        TEXTURE_SIZES.clear();
    }
}
