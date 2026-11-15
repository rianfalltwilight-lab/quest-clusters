package com.questclusters.platform;

import net.minecraft.client.Minecraft;

/** Minecraft client calls whose form differs between 1.21.1 and 1.20.1; client only. */
public final class Mc {
    private Mc() {
    }

    /** Fraction of the current game tick at this frame (0..1). */
    public static float partialTick() {
        return Minecraft.getInstance().getFrameTime();
    }
}
