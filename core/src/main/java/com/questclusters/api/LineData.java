package com.questclusters.api;

import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;

import java.util.List;
import java.util.Optional;

/**
 * Decorative lines, guide points (anchors) and hidden prerequisite lines of a chapter, wherever they come from:
 * the editor module registers a provider; without it, core reads Quest Enhance (if installed) by reflection.
 * Node keys use Quest Enhance's format, shared by all providers: "q:&lt;hex&gt;" quest, "l:&lt;hex&gt;" quest link,
 * "a:&lt;uuid&gt;" anchor (hex lower case, no zero padding).
 */
public interface LineData {
    /** Each decorative line as its node keys (drawn as spokes from the first node). */
    List<List<String>> decorativeLines(Chapter chapter);

    /** Node key of an anchor image, or empty if the image is not an anchor. */
    Optional<String> anchorKey(ChapterImage image);

    /** If the prerequisite line dependent → dependency is hidden: whether it shows on hover. Empty = not hidden. */
    Optional<Boolean> hiddenLine(Chapter chapter, Movable dependent, Movable dependency);

    static String questNode(long id) {
        return "q:" + Long.toUnsignedString(id, 16);
    }

    static String linkNode(long id) {
        return "l:" + Long.toUnsignedString(id, 16);
    }

    /** Node key of a canvas object (quest, quest link, anchor), or empty. */
    static Optional<String> nodeKey(Movable m, LineData data) {
        if (m instanceof Quest q) return Optional.of(questNode(q.id));
        if (m instanceof QuestLink l) return Optional.of(linkNode(l.id));
        if (m instanceof ChapterImage img) return data.anchorKey(img);
        return Optional.empty();
    }

    final class Holder {
        private static volatile LineData provider;

        private Holder() {
        }

        public static LineData get() {
            return provider;
        }

        public static void set(LineData p) {
            provider = p;
        }
    }
}
