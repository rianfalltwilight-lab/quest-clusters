package com.questclusters.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory set of clusters with an index from member id to cluster id.
 * Used both on the server (authoritative) and on the client (replica).
 * Not thread-safe: only touch it from the server or client main thread.
 */
public final class ClusterStore {
    private final Map<Long, QuestCluster> clusters = new LinkedHashMap<>();
    private final Map<Long, Long> clusterByMember = new HashMap<>();

    public Optional<QuestCluster> get(long id) {
        return Optional.ofNullable(clusters.get(id));
    }

    public boolean contains(long id) {
        return clusters.containsKey(id);
    }

    public Collection<QuestCluster> all() {
        return List.copyOf(clusters.values());
    }

    public int size() {
        return clusters.size();
    }

    public List<QuestCluster> byChapter(long chapterId) {
        List<QuestCluster> list = new ArrayList<>();
        for (QuestCluster c : clusters.values()) {
            if (c.chapterId() == chapterId) {
                list.add(c);
            }
        }
        return list;
    }

    /** @return the cluster containing this member, if any */
    public Optional<QuestCluster> clusterOfMember(long memberId) {
        Long id = clusterByMember.get(memberId);
        return id == null ? Optional.empty() : get(id);
    }

    /**
     * Adds or replaces a cluster. Callers must validate first; if a member already belongs to another cluster
     * it is moved to this one (the other cluster loses it), so the one-cluster-per-member rule always holds.
     */
    public void put(QuestCluster cluster) {
        remove(cluster.id());
        for (long member : cluster.members()) {
            Long previous = clusterByMember.get(member);
            if (previous != null && previous != cluster.id()) {
                QuestCluster other = clusters.get(previous);
                if (other != null) {
                    List<Long> remaining = new ArrayList<>(other.members());
                    remaining.remove(member);
                    clusters.put(other.id(), other.withMembers(remaining));
                }
            }
            clusterByMember.put(member, cluster.id());
        }
        clusters.put(cluster.id(), cluster);
    }

    public Optional<QuestCluster> remove(long id) {
        QuestCluster removed = clusters.remove(id);
        if (removed != null) {
            for (long member : removed.members()) {
                clusterByMember.remove(member, id);
            }
        }
        return Optional.ofNullable(removed);
    }

    public void replaceAll(Collection<QuestCluster> newClusters) {
        clear();
        newClusters.forEach(this::put);
    }

    public void clear() {
        clusters.clear();
        clusterByMember.clear();
    }
}
