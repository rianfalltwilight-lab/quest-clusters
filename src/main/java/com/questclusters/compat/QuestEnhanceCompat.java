package com.questclusters.compat;

import com.questclusters.QuestClusters;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Optional bridge to Quest Enhance's per-line "hidden dependency line" settings, read via reflection so there is
 * no compile-time dependency. If Quest Enhance is absent or its API changes, this quietly turns itself off.
 */
public final class QuestEnhanceCompat {
    public static final int HIDDEN_LINE_EDIT_COLOR = 0xFFC462FF; // Quest Enhance draws hidden lines this colour in edit mode

    private static boolean initialised;
    private static Method nodeKey;      // static String nodeKey(Movable)
    private static Method find;         // static Optional<Line> find(Chapter, String, String)
    private static Method revealOnHover; // Line#reveal_on_hover()

    private QuestEnhanceCompat() {
    }

    /** Hidden-line setting for one real line, if Quest Enhance has one. */
    public record LineSetting(boolean revealOnHover) {
    }

    public static boolean active() {
        init();
        return find != null;
    }

    public static Optional<LineSetting> hiddenLine(Chapter chapter, Movable dependent, Movable dependency) {
        if (!active() || !(dependent instanceof Quest || dependent instanceof QuestLink)
                || !(dependency instanceof Quest || dependency instanceof QuestLink)) {
            return Optional.empty();
        }
        try {
            Object a = nodeKey.invoke(null, dependent);
            Object b = nodeKey.invoke(null, dependency);
            Optional<?> line = (Optional<?>) find.invoke(null, chapter, a, b);
            if (line.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new LineSetting((Boolean) revealOnHover.invoke(line.get())));
        } catch (Throwable t) {
            QuestClusters.LOGGER.warn("Quest Enhance hidden-line lookup failed, disabling the integration", t);
            find = null;
            return Optional.empty();
        }
    }

    private static synchronized void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        if (!ModList.get().isLoaded("quest_enhance")) {
            return;
        }
        try {
            Class<?> c = Class.forName("com.quest_enhance.HiddenDependencyLines");
            Class<?> line = Class.forName("com.quest_enhance.HiddenDependencyLines$Line");
            nodeKey = c.getMethod("nodeKey", Movable.class);
            revealOnHover = line.getMethod("reveal_on_hover");
            find = c.getMethod("find", Chapter.class, String.class, String.class);
            QuestClusters.LOGGER.info("Quest Enhance detected: merged cluster lines follow its hidden-line settings");
        } catch (Throwable t) {
            QuestClusters.LOGGER.warn("Quest Enhance is loaded but its hidden-line API was not found; integration off ({})", t.toString());
            find = null;
        }
    }
}
