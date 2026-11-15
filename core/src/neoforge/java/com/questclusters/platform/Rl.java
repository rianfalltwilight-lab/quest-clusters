package com.questclusters.platform;

import net.minecraft.resources.ResourceLocation;

/** ResourceLocation factories (1.21 replaced the constructors with these). */
public final class Rl {
    private Rl() {
    }

    public static ResourceLocation of(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    /** Throws on invalid text, like the 1.20 constructor. */
    public static ResourceLocation parse(String id) {
        return ResourceLocation.parse(id);
    }
}
