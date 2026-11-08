package com.questclusters.editor.client.desc;

import com.questclusters.editor.client.GifTextures;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** An FTB Library icon that plays a GIF; any icon string ending in ".gif" becomes one (client side). */
public final class GifIcon extends Icon {
    private final ResourceLocation gif;

    public GifIcon(ResourceLocation gif) {
        this.gif = gif;
    }

    public ResourceLocation gif() {
        return gif;
    }

    @Override
    public void draw(GuiGraphics g, int x, int y, int w, int h) {
        if (!GifTextures.draw(g, gif, x, y, w, h, 0xFFFFFFFF)) {
            g.fill(x, y, x + w, y + h, 0xFF212121);
        }
    }

    @Override
    public String toString() {
        return gif.toString();
    }

    @Override
    public int hashCode() {
        return gif.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GifIcon g && g.gif.equals(gif);
    }
}
