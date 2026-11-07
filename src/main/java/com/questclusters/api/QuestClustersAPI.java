package com.questclusters.api;

import com.questclusters.QuestClusters;
import com.questclusters.client.ClientClusters;
import com.questclusters.data.ClusterStore;
import com.questclusters.data.QuestCluster;
import com.questclusters.server.ServerClusters;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Public API, also exposed to KubeJS scripts as {@code QuestClusters}.
 * <p>
 * Ids may be given as numbers or as FTB Quests hex strings ("0123456789ABCDEF" or "#0123..."). Reads use the
 * server's data on the server thread and the client's synced copy elsewhere. Changes are server-only, skip the
 * edit-mode permission check (scripts are trusted) but go through the same validation and sync as editor changes.
 * <pre>
 * // server_scripts
 * QuestClusters.create("0123456789ABCDEF", "Ore processing", ["...", "..."])
 * QuestClusters.ofMember(questId)        // cluster or null
 * QuestClusters.inChapter(chapterId)     // list
 * QuestClusters.setMembers(clusterId, [...])
 * QuestClusters.dissolve(clusterId)
 * </pre>
 */
public final class QuestClustersAPI {
    private QuestClustersAPI() {
    }

    // ------------------------------------------------------------------ reads

    public static List<QuestCluster> all() {
        return List.copyOf(store().all());
    }

    public static List<QuestCluster> inChapter(Object chapterId) {
        return store().byChapter(id(chapterId));
    }

    /** @return the cluster containing this quest / quest link / chapter image, or null */
    public static QuestCluster ofMember(Object objectId) {
        return store().clusterOfMember(id(objectId)).orElse(null);
    }

    public static QuestCluster get(Object clusterId) {
        return store().get(id(clusterId)).orElse(null);
    }

    public static String idString(long id) {
        return QuestCluster.idToString(id);
    }

    // ------------------------------------------------------------------ changes (server only)

    /** @return the new cluster, or null if rejected (reason is logged) */
    public static QuestCluster create(Object chapterId, String title, Collection<?> memberIds) {
        ServerQuestFile file = serverFile("create");
        if (file == null) {
            return null;
        }
        QuestCluster c = ServerClusters.draft(file, id(chapterId), title, ids(memberIds));
        return ServerClusters.applyChecked(file, c).map(err -> rejected("create", err)).orElse(c);
    }

    public static QuestCluster setMembers(Object clusterId, Collection<?> memberIds) {
        return change(clusterId, c -> c.withMembers(ids(memberIds)), "setMembers");
    }

    public static QuestCluster setTitle(Object clusterId, String title, String subtitle) {
        return change(clusterId, c -> c.withText(title, subtitle == null ? c.subtitle() : subtitle), "setTitle");
    }

    public static QuestCluster setDefaultExpanded(Object clusterId, boolean expanded) {
        return change(clusterId, c -> c.withDefaultExpanded(expanded), "setDefaultExpanded");
    }

    public static boolean dissolve(Object clusterId) {
        return serverFile("dissolve") != null && ServerClusters.dissolve(id(clusterId));
    }

    // ------------------------------------------------------------------ helpers

    private static QuestCluster change(Object clusterId, java.util.function.UnaryOperator<QuestCluster> edit, String what) {
        ServerQuestFile file = serverFile(what);
        if (file == null) {
            return null;
        }
        QuestCluster current = ServerClusters.store().get(id(clusterId)).orElse(null);
        if (current == null) {
            QuestClusters.LOGGER.warn("QuestClusters.{}: no cluster {}", what, clusterId);
            return null;
        }
        QuestCluster next = edit.apply(current);
        return ServerClusters.applyChecked(file, next).map(err -> rejected(what, err)).orElse(next);
    }

    private static QuestCluster rejected(String what, com.questclusters.data.ValidationError err) {
        QuestClusters.LOGGER.warn("QuestClusters.{} rejected: {}", what,
                Component.translatable(err.translationKey(), err.args()).getString());
        return null;
    }

    private static ServerQuestFile serverFile(String what) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread() || ServerQuestFile.getInstance().isEmpty()) {
            QuestClusters.LOGGER.warn("QuestClusters.{} must be called on the server thread", what);
            return null;
        }
        return ServerQuestFile.getInstance().get();
    }

    private static ClusterStore store() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread()) {
            return ServerClusters.store();
        }
        return FMLEnvironment.dist.isClient() ? ClientClusters.store() : ServerClusters.store();
    }

    static long id(Object o) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        return o == null ? 0L : QuestCluster.idFromString(o.toString().trim());
    }

    private static List<Long> ids(Collection<?> list) {
        List<Long> out = new ArrayList<>();
        if (list != null) {
            for (Object o : list) {
                long id = id(o);
                if (id != 0L && !out.contains(id)) {
                    out.add(id);
                }
            }
        }
        return out;
    }
}
