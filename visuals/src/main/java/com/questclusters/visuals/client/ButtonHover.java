package com.questclusters.visuals.client;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.client.gui.GuiGraphics;

/** Shared by the chapter panel button mixins: animated hover highlight with the configured glow strength. */
public final class ButtonHover {
    private ButtonHover() {
    }

    /** Replaces FTB Quests' WHITE.withAlpha(40) highlight while hovered. */
    public static int alpha(Ease ease) {
        int glow = VisualsConfig.BUTTON_HOVER_GLOW_ALPHA.get();
        return VisualsConfig.PANEL_BUTTON_HOVER.get() ? Math.round(glow * Math.max(0.2F, ease.get())) : glow;
    }

    /** After drawing: keep fading the highlight out once the mouse has left. */
    public static void fadeOut(Ease ease, boolean hovered, GuiGraphics g, int x, int y, int w, int h) {
        ease.set(hovered ? 1F : 0F);
        if (!hovered && VisualsConfig.PANEL_BUTTON_HOVER.get()) {
            float f = ease.get();
            if (f > 0.01F) {
                Color4I.WHITE.withAlpha(Math.round(VisualsConfig.BUTTON_HOVER_GLOW_ALPHA.get() * f)).draw(g, x, y, w, h);
            }
        }
    }
}
