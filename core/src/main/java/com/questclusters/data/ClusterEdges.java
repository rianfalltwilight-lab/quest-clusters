package com.questclusters.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.LongFunction;

/**
 * Works out which dependency lines have to be drawn to or from collapsed cluster boxes.
 * <p>
 * Mirrors FTB Quests' rule: an object A draws a line to every object B on the canvas whose quest is a dependency of
 * A's quest. When A or B is hidden inside a collapsed cluster, that end is replaced by the cluster box. Lines where
 * neither end is a cluster are left to FTB Quests. Duplicates collapse into one line.
 */
public final class ClusterEdges {
    private ClusterEdges() {
    }

    /**
     * One object on the canvas: a quest, or a quest link (whose {@code questId} is the linked quest).
     *
     * @param movableId          id of the quest or quest link object
     * @param questId            the quest it shows
     * @param dependencyQuestIds quests this one depends on
     * @param visible            whether the player can see it (FTB Quests skips lines of invisible quests)
     * @param hideOwnLines       quest's "hide dependency lines" flag
     * @param hideDependentLines quest's "hide dependent lines" flag (lines from quests depending on it)
     */
    public record Node(long movableId, long questId, Set<Long> dependencyQuestIds,
                       boolean visible, boolean hideOwnLines, boolean hideDependentLines) {
    }

    /** End of a line: either a canvas object, or a collapsed cluster box. */
    public record End(boolean cluster, long id) {
        public static End object(long id) {
            return new End(false, id);
        }

        public static End box(long id) {
            return new End(true, id);
        }
    }

    /** A line from the dependent end to the dependency end. */
    public record Edge(End dependent, End dependency) {
    }

    /** One real dependency line (canvas object to canvas object) that a merged edge stands for. */
    public record Pair(long dependentMovable, long dependencyMovable) {
    }

    /**
     * @param collapsedClusterOf for a movable id, the id of the collapsed cluster hiding it, or empty
     */
    public static List<Edge> compute(List<Node> nodes, LongFunction<OptionalLong> collapsedClusterOf) {
        return List.copyOf(computeWithPairs(nodes, collapsedClusterOf).keySet());
    }

    /**
     * Same as {@link #compute}, but also returns, for every merged edge, the real object-to-object lines it replaces
     * (used to honour per-line settings of other addons, e.g. Quest Enhance's hidden lines).
     */
    public static Map<Edge, List<Pair>> computeWithPairs(List<Node> nodes, LongFunction<OptionalLong> collapsedClusterOf) {
        Map<Long, List<Node>> byQuest = new HashMap<>();
        for (Node n : nodes) {
            byQuest.computeIfAbsent(n.questId(), k -> new ArrayList<>()).add(n);
        }

        Map<Edge, List<Pair>> edges = new LinkedHashMap<>();
        for (Node a : nodes) {
            if (!a.visible() || a.hideOwnLines()) {
                continue;
            }
            End from = endOf(a, collapsedClusterOf);
            for (long dep : a.dependencyQuestIds()) {
                for (Node b : byQuest.getOrDefault(dep, List.of())) {
                    if (!b.visible() || b.hideDependentLines()) {
                        continue;
                    }
                    End to = endOf(b, collapsedClusterOf);
                    if ((from.cluster() || to.cluster()) && !from.equals(to)) {
                        edges.computeIfAbsent(new Edge(from, to), k -> new ArrayList<>())
                                .add(new Pair(a.movableId(), b.movableId()));
                    }
                }
            }
        }
        return edges;
    }

    private static End endOf(Node n, LongFunction<OptionalLong> collapsedClusterOf) {
        OptionalLong c = collapsedClusterOf.apply(n.movableId());
        return c.isPresent() ? End.box(c.getAsLong()) : End.object(n.movableId());
    }
}
