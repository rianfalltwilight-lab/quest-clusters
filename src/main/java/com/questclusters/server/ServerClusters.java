package com.questclusters.server;

import com.questclusters.QuestClusters;
import com.questclusters.data.ClusterPruner;
import com.questclusters.data.ClusterStore;
import com.questclusters.data.ClusterValidator;
import com.questclusters.data.QuestCluster;
import com.questclusters.data.ValidationError;
import com.questclusters.net.ClusterNetwork;
import dev.ftb.mods.ftbquests.integration.PermissionsHelper;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Authoritative server-side cluster state. Lifecycle is driven by mixins into {@link ServerQuestFile}:
 * load after FTB Quests loads, save whenever FTB Quests saves, prune after any FTB Quests edit.
 */
public final class ServerClusters {
    private static final ClusterStore STORE = new ClusterStore();
    private static boolean dirty;
    private static boolean pruneRequested;

    private ServerClusters() {
    }

    public static ClusterStore store() {
        return STORE;
    }

    // ---------------------------------------------------------------- lifecycle (called from mixins)

    public static void onQuestsLoaded(ServerQuestFile file) {
        Path folder = file.getFolder();
        STORE.replaceAll(folder == null ? List.of() : ClusterStorage.load(folder));
        dirty = false;

        ClusterPruner.Result r = ClusterPruner.prune(STORE, new FtbqMemberLookup(file));
        if (r.changed()) {
            QuestClusters.LOGGER.warn("Dropped {} stale quest cluster(s) and trimmed {} on load", r.removed().size(), r.updated().size());
            dirty = true;
        }
        pruneRequested = false;
        QuestClusters.LOGGER.info("Loaded {} quest cluster(s)", STORE.size());

        // /ftbquests reload: players are already online and need the fresh data
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ClusterNetwork.sendFullSyncToAll(server);
        }
    }

    public static void onQuestsSaving(ServerQuestFile file) {
        if (pruneRequested) {
            runPrune(file);
        }
        if (dirty && file.getFolder() != null) {
            ClusterStorage.save(file.getFolder(), STORE.all());
            dirty = false;
        }
    }

    public static void onQuestFileDirty() {
        pruneRequested = true;
    }

    public static void onUnload() {
        STORE.clear();
        dirty = false;
        pruneRequested = false;
    }

    /** Server tick: apply pending prunes so clients learn about removed members promptly. */
    public static void tick() {
        if (pruneRequested) {
            ServerQuestFile.getInstance().filter(f -> !f.isLoading()).ifPresent(ServerClusters::runPrune);
        }
    }

    private static void runPrune(ServerQuestFile file) {
        pruneRequested = false;
        ClusterPruner.Result r = ClusterPruner.prune(STORE, new FtbqMemberLookup(file));
        if (r.changed()) {
            dirty = true;
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                r.updated().forEach(c -> ClusterNetwork.sendUpsertToAll(server, c));
                r.removed().forEach(id -> ClusterNetwork.sendRemoveToAll(server, id));
            }
        }
    }

    // ---------------------------------------------------------------- edits requested by clients

    public static void handleCreate(ServerPlayer player, long chapterId, String title, List<Long> members) {
        withEditableFile(player, file -> {
            apply(player, file, draft(file, chapterId, title, members));
        });
    }

    public static void handleUpdate(ServerPlayer player, QuestCluster cluster) {
        withEditableFile(player, file -> {
            if (!STORE.contains(cluster.id())) {
                reject(player, new ValidationError("no_cluster"));
                return;
            }
            apply(player, file, cluster);
        });
    }

    /** Removes the cluster only; member quests are kept where they are. */
    public static void handleDissolve(ServerPlayer player, long clusterId) {
        withEditableFile(player, file -> {
            if (!dissolve(clusterId)) {
                reject(player, new ValidationError("no_cluster"));
            }
        });
    }

    /**
     * Pasting a multi-selection creates new objects at known positions; find them and rebuild each cluster.
     * Packets are handled in order, so the copies already exist when this arrives.
     */
    public static void handlePaste(ServerPlayer player, long chapterId, List<com.questclusters.data.ClusterTemplate> templates) {
        withEditableFile(player, file -> {
            dev.ftb.mods.ftbquests.quest.Chapter chapter = file.getChapter(chapterId);
            if (chapter == null) {
                return;
            }
            for (var t : templates) {
                List<Long> members = new ArrayList<>();
                for (var spec : t.members()) {
                    findPastedCopy(file, chapter, spec, members).ifPresent(members::add);
                }
                if (members.isEmpty()) {
                    QuestClusters.LOGGER.debug("Paste: no copies found for cluster {}", t.title());
                    continue;
                }
                apply(player, file, new QuestCluster(newId(), chapterId, t.title(), t.subtitle(), t.color(),
                        t.defaultExpanded(), t.x(), t.y(), t.width(), t.height(), members));
            }
        });
    }

    private static Optional<Long> findPastedCopy(ServerQuestFile file, dev.ftb.mods.ftbquests.quest.Chapter chapter,
                                                 com.questclusters.data.ClusterTemplate.MemberSpec spec, List<Long> taken) {
        var source = file.getBase(spec.sourceId());
        List<Movable> candidates = new ArrayList<>();
        chapter.getQuests().forEach(candidates::add);
        chapter.getImages().forEach(candidates::add);
        Movable best = null;
        for (Movable m : candidates) {
            if (m.getMovableID() == spec.sourceId() || taken.contains(m.getMovableID())
                    || STORE.clusterOfMember(m.getMovableID()).isPresent()
                    || Math.abs(m.getX() - spec.x()) > 1e-3 || Math.abs(m.getY() - spec.y()) > 1e-3
                    || (source != null && source.getClass() != m.getClass())) {
                continue;
            }
            boolean sameTitle = source != null && m.getTitle().getString().equals(source.getTitle().getString());
            if (best == null || sameTitle) {
                best = m; // later entries are newer; a matching title wins
            }
        }
        return Optional.ofNullable(best).map(Movable::getMovableID);
    }

    private static void apply(ServerPlayer player, ServerQuestFile file, QuestCluster cluster) {
        applyChecked(file, cluster).ifPresent(err -> reject(player, err));
    }

    /** Validates, stores, saves and broadcasts. Returns the reason if rejected. */
    public static Optional<ValidationError> applyChecked(ServerQuestFile file, QuestCluster cluster) {
        Optional<ValidationError> err = ClusterValidator.validate(cluster, STORE, new FtbqMemberLookup(file));
        if (err.isPresent()) {
            return err;
        }
        STORE.put(cluster);
        dirty = true;
        file.markDirty();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ClusterNetwork.sendUpsertToAll(server, cluster);
        }
        return Optional.empty();
    }

    /** Removes a cluster (members are kept). Returns false if it did not exist. */
    public static boolean dissolve(long clusterId) {
        if (STORE.remove(clusterId).isEmpty()) {
            return false;
        }
        dirty = true;
        ServerQuestFile.getInstance().ifPresent(ServerQuestFile::markDirty);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ClusterNetwork.sendRemoveToAll(server, clusterId);
        }
        return true;
    }

    /** New cluster with its box centred on the members. */
    public static QuestCluster draft(ServerQuestFile file, long chapterId, String title, List<Long> members) {
        return withDefaultBox(file, new QuestCluster(newId(), chapterId, title.strip(), "", QuestCluster.DEFAULT_COLOR,
                true, 0, 0, 3, 1, members));
    }

    private static void withEditableFile(ServerPlayer player, java.util.function.Consumer<ServerQuestFile> action) {
        Optional<ServerQuestFile> file = ServerQuestFile.getInstance();
        if (file.isEmpty() || file.get().isLoading()) {
            return;
        }
        if (!PermissionsHelper.canPlayerEdit(player)) {
            reject(player, new ValidationError("no_permission"));
            return;
        }
        action.accept(file.get());
    }

    private static void reject(ServerPlayer player, ValidationError error) {
        QuestClusters.LOGGER.debug("Rejected cluster edit from {}: {}", player.getGameProfile().getName(), error.key());
        player.displayClientMessage(Component.translatable(error.translationKey(), error.args()).withStyle(ChatFormatting.RED), false);
    }

    private static long newId() {
        long id;
        do {
            id = ThreadLocalRandom.current().nextLong() & Long.MAX_VALUE;
        } while (id == 0L || id == 1L || STORE.contains(id));
        return id;
    }

    /** Places the collapsed box at the centre of the members' bounding box. */
    private static QuestCluster withDefaultBox(ServerQuestFile file, QuestCluster c) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        List<Long> movables = new ArrayList<>();
        for (long id : c.members()) {
            if (file.getBase(id) instanceof Movable m) {
                movables.add(id);
                minX = Math.min(minX, m.getX() - m.getWidth() / 2D);
                maxX = Math.max(maxX, m.getX() + m.getWidth() / 2D);
                minY = Math.min(minY, m.getY() - m.getHeight() / 2D);
                maxY = Math.max(maxY, m.getY() + m.getHeight() / 2D);
            }
        }
        if (movables.isEmpty()) {
            return c;
        }
        return new QuestCluster(c.id(), c.chapterId(), c.title(), c.subtitle(), c.color(), c.defaultExpanded(),
                (minX + maxX) / 2D, (minY + maxY) / 2D, 3D, 1D, c.members());
    }
}
