package com.questclusters.compat;

import com.questclusters.QuestClusters;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * Optional bridge to Certain Questing Additions' animation settings, so cluster boxes animate like CQA's quest
 * buttons. Read via reflection (CQA is all-rights-reserved; no code of it is used). Without CQA, sensible defaults.
 */
public final class CqaCompat {
    private static final String CONFIG = "ru.hollowhorizon.additions.questing.config.QuestAnimationsConfig";
    private static boolean initialised;
    private static Object questHover, glowAlpha, smooth;
    private static Method boolGet, intGet;
    private static long cacheTime;
    private static boolean cachedHover = true, cachedSmooth = true;
    private static int cachedGlow = 60;

    private CqaCompat() {
    }

    public static boolean loaded() {
        init();
        return questHover != null;
    }

    /** CQA's "quest_hover" switch; true without CQA. */
    public static boolean hoverAnimation() {
        var v = com.questclusters.api.VisualSettings.Holder.get();
        if (v != null) return v.questHover();
        refresh();
        return cachedHover;
    }

    /** CQA's "quest_hover_glow_alpha" (0-255); 60 without CQA. */
    public static int hoverGlowAlpha() {
        var v = com.questclusters.api.VisualSettings.Holder.get();
        if (v != null) return v.questHoverGlowAlpha();
        refresh();
        return cachedGlow;
    }

    /** CQA's "smooth_animations" (easing); true without CQA. */
    public static boolean smooth() {
        var v = com.questclusters.api.VisualSettings.Holder.get();
        if (v != null) return v.smoothAnimations();
        refresh();
        return cachedSmooth;
    }

    private static void refresh() {
        long now = System.currentTimeMillis();
        if (now - cacheTime < 1000L || !loaded()) {
            return;
        }
        cacheTime = now;
        try {
            cachedHover = (Boolean) boolGet.invoke(questHover);
            cachedSmooth = (Boolean) boolGet.invoke(smooth);
            cachedGlow = (Integer) intGet.invoke(glowAlpha);
        } catch (Throwable t) {
            QuestClusters.LOGGER.warn("Reading CQA settings failed, using defaults ({})", t.toString());
            questHover = null;
        }
    }

    private static synchronized void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        if (!ModList.get().isLoaded("certain_questing_additions")) {
            return;
        }
        try {
            Class<?> c = Class.forName(CONFIG);
            questHover = c.getField("QUEST_HOVER").get(null);
            smooth = c.getField("SMOOTH_ANIMATIONS").get(null);
            glowAlpha = c.getField("QUEST_HOVER_GLOW_ALPHA").get(null);
            boolGet = questHover.getClass().getMethod("get");
            intGet = glowAlpha.getClass().getMethod("get");
            QuestClusters.LOGGER.info("Certain Questing Additions detected: cluster boxes follow its hover animation settings");
        } catch (Throwable t) {
            QuestClusters.LOGGER.warn("CQA is loaded but its config was not found; using defaults ({})", t.toString());
            questHover = null;
        }
    }
}
