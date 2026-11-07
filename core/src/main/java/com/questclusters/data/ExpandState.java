package com.questclusters.data;

import java.util.HashMap;
import java.util.Map;

/**
 * A player's own expand/collapse choices, overriding each cluster's author default.
 * Pure logic; persistence lives in {@code client.ClusterViewState}.
 */
public final class ExpandState {
    private final Map<Long, Boolean> overrides = new HashMap<>();
    private int version;

    public boolean isExpanded(QuestCluster cluster) {
        Boolean o = overrides.get(cluster.id());
        return o != null ? o : cluster.defaultExpanded();
    }

    /** @return true if this changed anything */
    public boolean setExpanded(QuestCluster cluster, boolean expanded) {
        Boolean previous = overrides.get(cluster.id());
        boolean was = previous != null ? previous : cluster.defaultExpanded();
        if (expanded == cluster.defaultExpanded()) {
            overrides.remove(cluster.id()); // back to author default: no need to remember
        } else {
            overrides.put(cluster.id(), expanded);
        }
        if (was != expanded) {
            version++;
            return true;
        }
        return false;
    }

    /** Is this member hidden because its cluster is collapsed? */
    public boolean isHidden(ClusterStore store, long memberId) {
        return store.clusterOfMember(memberId).map(c -> !isExpanded(c)).orElse(false);
    }

    /** Increases whenever any cluster's effective state changes. */
    public int version() {
        return version;
    }

    public Map<Long, Boolean> overrides() {
        return Map.copyOf(overrides);
    }

    public void load(Map<Long, Boolean> saved) {
        overrides.clear();
        overrides.putAll(saved);
        version++;
    }

    public void clear() {
        overrides.clear();
        version++;
    }
}
