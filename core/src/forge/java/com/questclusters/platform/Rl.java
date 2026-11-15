package com.questclusters.platform;

import net.minecraft.resources.ResourceLocation;

/** ResourceLocation factories (1.21 replaced the constructors with these). */
public final class Rl {
    private Rl() {
    }

    public static ResourceLocation of(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation mc(String path) {
        return new ResourceLocation("minecraft", path);
    }

    /** Throws on invalid text. */
    public static ResourceLocation parse(String id) {
        return new ResourceLocation(id);
    }
}
