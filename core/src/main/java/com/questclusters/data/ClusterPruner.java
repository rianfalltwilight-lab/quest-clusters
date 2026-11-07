package com.questclusters.data;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * Brings the store back in line with the quest file after edits made through FTB Quests itself
 * (deleting quests or chapters, moving objects to another chapter, undo/redo).
 */
public final class ClusterPruner {
    private ClusterPruner() {
    }

    /**
     * @param updated         clusters that lost members but still exist
     * @param removed         ids of clusters that were removed (chapter gone, or no members left)
     * @param removedClusters the removed clusters as they were just before removal
     * @param droppedMembers  for clusters that still exist: their state before, and the members they lost
     */
    public record Result(List<QuestCluster> updated, List<Long> removed, List<QuestCluster> removedClusters,
                         java.util.Map<QuestCluster, List<Long>> droppedMembers) {
        public boolean changed() {
            return !updated.isEmpty() || !removed.isEmpty();
        }
    }

    public static Result prune(ClusterStore store, MemberLookup lookup) {
        List<QuestCluster> updated = new ArrayList<>();
        List<Long> removed = new ArrayList<>();
        List<QuestCluster> removedClusters = new ArrayList<>();
        java.util.Map<QuestCluster, List<Long>> dropped = new java.util.LinkedHashMap<>();

        for (QuestCluster c : store.all()) {
            if (!lookup.chapterExists(c.chapterId())) {
                store.remove(c.id());
                removed.add(c.id());
                removedClusters.add(c);
                continue;
            }

            List<Long> kept = new ArrayList<>(c.members().size());
            for (long member : c.members()) {
                OptionalLong chapter = lookup.chapterOfMovable(member);
                if (chapter.isPresent() && chapter.getAsLong() == c.chapterId()) {
                    kept.add(member);
                }
            }

            if (kept.isEmpty()) {
                store.remove(c.id());
                removed.add(c.id());
                removedClusters.add(c);
            } else if (kept.size() != c.members().size()) {
                QuestCluster pruned = c.withMembers(kept);
                store.put(pruned);
                updated.add(pruned);
                dropped.put(c, c.members().stream().filter(m -> !kept.contains(m)).toList());
            }
        }
        return new Result(updated, removed, removedClusters, dropped);
    }
}
