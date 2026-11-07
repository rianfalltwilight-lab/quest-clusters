package com.questclusters.compat;

import com.questclusters.QuestClusters;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import net.neoforged.fml.ModList;

import com.questclusters.api.LineData;
import dev.ftb.mods.ftbquests.quest.ChapterImage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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

    // decorative lines (polylines through quests, quest links and "anchor" chapter images)
    private static Method decoGet;        // static List<Line> DecorativeDependencyLines.get(Chapter)
    private static Method decoNodes;      // Line#nodes() -> List<String>
    private static Method decoQuestNode;  // static String questNode(long)
    private static Method decoLinkNode;   // static String questLinkNode(long)
    private static Method anchorNodeKey;  // static Optional<String> DecorativeAnchor.nodeKey(ChapterImage)

    private QuestEnhanceCompat() {
    }

    public static boolean decorativeLinesActive() {
        var p = provider();
        return p != null && (p != REFLECT || decoGet != null);
    }

    /**
     * Anchor images that only serve lines running between members of the given set: they belong to the cluster
     * visually, so they move and hide with it. Returns their chapter image ids.
     */
    public static java.util.Set<Long> anchorsInside(Chapter chapter, java.util.Collection<Long> memberIds) {
        java.util.Set<Long> result = new java.util.HashSet<>();
        var p = provider();
        if (p == null || chapter == null || !decorativeLinesActive()) {
            return result;
        }
        java.util.Set<String> memberKeys = new java.util.HashSet<>();
        for (long id : memberIds) {
            memberKeys.add(com.questclusters.api.LineData.questNode(id));
            memberKeys.add(com.questclusters.api.LineData.linkNode(id));
        }
        java.util.Map<String, Long> anchorIds = new java.util.HashMap<>();
        for (dev.ftb.mods.ftbquests.quest.ChapterImage img : chapter.getImages()) {
            p.anchorKey(img).ifPresent(k -> anchorIds.put(k, img.id));
        }
        if (anchorIds.isEmpty()) {
            return result;
        }
        java.util.Set<String> insideAnchors = new java.util.HashSet<>(), outsideAnchors = new java.util.HashSet<>();
        for (java.util.List<String> nodes : p.decorativeLines(chapter)) {
            boolean hasMember = false, allInside = true;
            for (String n : nodes) {
                if (anchorIds.containsKey(n)) continue;
                if (memberKeys.contains(n)) hasMember = true;
                else allInside = false;
            }
            for (String n : nodes) {
                if (anchorIds.containsKey(n)) (hasMember && allInside ? insideAnchors : outsideAnchors).add(n);
            }
        }
        insideAnchors.removeAll(outsideAnchors); // an anchor shared with an outside line stays put
        insideAnchors.forEach(k -> result.add(anchorIds.get(k)));
        return result;
    }

    /** Hidden-line setting for one real line. */
    public record LineSetting(boolean revealOnHover) {
    }

    /** Editor module's data if registered, else Quest Enhance (reflection) if installed, else null. */
    public static LineData provider() {
        LineData p = LineData.Holder.get();
        if (p != null) return p;
        init();
        return find != null ? REFLECT : null;
    }

    public static boolean active() {
        return provider() != null;
    }

    public static Optional<LineSetting> hiddenLine(Chapter chapter, Movable dependent, Movable dependency) {
        LineData p = provider();
        if (p == null || !(dependent instanceof Quest || dependent instanceof QuestLink)
                || !(dependency instanceof Quest || dependency instanceof QuestLink)) {
            return Optional.empty();
        }
        return p.hiddenLine(chapter, dependent, dependency).map(LineSetting::new);
    }

    private static final LineData REFLECT = new LineData() {
        @Override
        public List<List<String>> decorativeLines(Chapter chapter) {
            List<List<String>> out = new ArrayList<>();
            if (decoGet == null || chapter == null) return out;
            try {
                for (Object line : (List<?>) decoGet.invoke(null, chapter)) {
                    @SuppressWarnings("unchecked")
                    List<String> nodes = (List<String>) decoNodes.invoke(line);
                    out.add(nodes);
                }
            } catch (Throwable t) {
                QuestClusters.LOGGER.warn("Quest Enhance decorative-line lookup failed, disabling that part", t);
                decoGet = null;
            }
            return out;
        }

        @Override
        public Optional<String> anchorKey(ChapterImage image) {
            if (anchorNodeKey == null || decoGet == null) return Optional.empty();
            try {
                return ((Optional<?>) anchorNodeKey.invoke(null, image)).map(Object::toString);
            } catch (Throwable t) {
                return Optional.empty();
            }
        }

        @Override
        public Optional<Boolean> hiddenLine(Chapter chapter, Movable dependent, Movable dependency) {
            if (find == null) return Optional.empty();
            try {
                Object a = nodeKey.invoke(null, dependent);
                Object b = nodeKey.invoke(null, dependency);
                Optional<?> line = (Optional<?>) find.invoke(null, chapter, a, b);
                if (line.isEmpty()) return Optional.empty();
                return Optional.of((Boolean) revealOnHover.invoke(line.get()));
            } catch (Throwable t) {
                QuestClusters.LOGGER.warn("Quest Enhance hidden-line lookup failed, disabling the integration", t);
                find = null;
                return Optional.empty();
            }
        }
    };

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
            try {
                Class<?> d = Class.forName("com.quest_enhance.DecorativeDependencyLines");
                Class<?> dl = Class.forName("com.quest_enhance.DecorativeDependencyLines$Line");
                Class<?> a = Class.forName("com.quest_enhance.DecorativeAnchor");
                decoQuestNode = d.getMethod("questNode", long.class);
                decoLinkNode = d.getMethod("questLinkNode", long.class);
                decoNodes = dl.getMethod("nodes");
                anchorNodeKey = a.getMethod("nodeKey", dev.ftb.mods.ftbquests.quest.ChapterImage.class);
                decoGet = d.getMethod("get", Chapter.class);
            } catch (Throwable t) {
                QuestClusters.LOGGER.warn("Quest Enhance decorative-line API not found; anchors won't follow clusters ({})", t.toString());
                decoGet = null;
            }
        } catch (Throwable t) {
            QuestClusters.LOGGER.warn("Quest Enhance is loaded but its hidden-line API was not found; integration off ({})", t.toString());
            find = null;
        }
    }
}
