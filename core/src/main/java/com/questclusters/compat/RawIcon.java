package com.questclusters.compat;

import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Stand-in for an icon string only an optional client module can draw (e.g. "entity:..." from the visuals
 * module). Draws nothing, but keeps the original string, so FTB Quests saves it back unchanged.
 */
public final class RawIcon extends Icon {
    private final String raw;

    public RawIcon(String raw) {
        this.raw = raw;
    }

    @Override
    public void draw(GuiGraphics graphics, int x, int y, int w, int h) {
    }

    @Override
    public String toString() {
        return raw;
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RawIcon r && r.raw.equals(raw);
    }
}
