package com.questclusters.editor.perf;

import dev.ftb.mods.ftbquests.quest.Quest;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Client-side, per-tick cache of "is this quest visible to my team": the quest screen asks for every quest and every
 * dependency line each frame, and the answer can walk long dependency chains. Cleared every client tick.
 */
public final class VisibilityCache {
    private static final Map<Quest, Boolean> CACHE = new IdentityHashMap<>();
    /** Dev self-test statistics: dependency lines skipped because they were off screen. */
    public static int culledLines;

    private VisibilityCache() {
    }

    public static Boolean get(Quest q) {
        return CACHE.get(q);
    }

    public static void put(Quest q, boolean visible) {
        CACHE.put(q, visible);
    }

    public static void clear() {
        CACHE.clear();
    }

    public static int size() {
        return CACHE.size();
    }
}
