package com.questclusters.client;

import com.questclusters.data.ClusterStore;
import com.questclusters.data.QuestCluster;

import java.util.List;

/**
 * Client replica of the server's clusters. Kept separate from FTB Quests' client file, so it does not matter
 * whether our sync packet arrives before or after FTB Quests' own sync.
 * Contains no client-only Minecraft classes, so it is safe to reference from common packet registration.
 */
public final class ClientClusters {
    private static final ClusterStore STORE = new ClusterStore();
    private static int revision;

    private ClientClusters() {
    }

    public static ClusterStore store() {
        return STORE;
    }

    /** Increases on every change; the quest screen (phase 1) compares it to know when to rebuild widgets. */
    public static int revision() {
        return revision;
    }

    public static void replaceAll(List<QuestCluster> clusters) {
        STORE.replaceAll(clusters);
        revision++;
    }

    public static void upsert(QuestCluster cluster) {
        STORE.put(cluster);
        revision++;
    }

    public static void remove(long id) {
        STORE.remove(id);
        revision++;
    }

    public static void clear() {
        STORE.clear();
        revision++;
    }
}
