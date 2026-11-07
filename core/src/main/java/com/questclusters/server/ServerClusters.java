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
    private static final com.questclusters.data.ClusterRevival REVIVAL = new com.questclusters.data.ClusterRevival();
    /** Last seen bounds of each cluster's members (cx, cy, w, h): lets the box follow when they all move together. */
    private static final java.util.Map<Long, double[]> LAST_BOUNDS = new java.util.HashMap<>();
    /** Clusters whose box was just set explicitly (move/undo/redo): only refresh their baseline. */
    private static final java.util.Set<Long> SKIP_FOLLOW = new java.util.HashSet<>();

    private ServerClusters() {
    }

    public static ClusterStore store() {
        return STORE;
    }

    // ---------------------------------------------------------------- lifecycle (called from mixins)

    public static com.questclusters.migration.MigrationRunner.Result lastMigration;

    public static void onQuestsLoaded(ServerQuestFile file) {
        Path folder = file.getFolder();
        // other addons' leftovers are read from the raw files first, before anything re-saves them
        if (folder != null) {
            lastMigration = com.questclusters.migration.MigrationRunner.run(
                    new com.questclusters.migration.MigrationContext(folder, net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get(), file),
                    com.questclusters.migration.MigrationRegistry.all(), System.currentTimeMillis());
        }
        STORE.replaceAll(folder == null ? List.of() : ClusterStorage.load(folder));
        dirty = false;

        ClusterPruner.Result r = ClusterPruner.prune(STORE, new FtbqMemberLookup(file));
        if (r.changed()) {
            QuestClusters.LOGGER.warn("Dropped {} stale quest cluster(s) and trimmed {} on load", r.removed().size(), r.updated().size());
            dirty = true;
        }
        pruneRequested = false;
        QuestClusters.LOGGER.info("Loaded {} quest cluster(s)", STORE.size());

        ServerScoring.load(file);
        com.questclusters.api.QuestBookHooks.fireLoaded(file);

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
        ServerScoring.save(file);
        com.questclusters.api.QuestBookHooks.fireSaving(file);
    }

    public static void onQuestFileDirty() {
        pruneRequested = true;
    }

    public static void onUnload() {
        ServerScoring.unload();
        com.questclusters.api.QuestBookHooks.fireUnloaded();
        STORE.clear();
        REVIVAL.clear();
        LAST_BOUNDS.clear();
        SKIP_FOLLOW.clear();
        dirty = false;
        pruneRequested = false;
    }

    /** Server tick: apply pending prunes so clients learn about removed members promptly. */
    private static int tickCounter;

    public static void tick() {
        MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
        if (srv != null) {
            ServerScoring.tick(srv);
            ServerQuestFile.getInstance().filter(f -> !f.isLoading()).ifPresent(com.questclusters.api.QuestBookHooks::fireTick);
        }
        if (++tickCounter % 20 == 0) {
            pruneRequested = true;
        }
        if (pruneRequested) {
            ServerQuestFile.getInstance().filter(f -> !f.isLoading()).ifPresent(ServerClusters::runPrune);
        }
    }

    private static void runPrune(ServerQuestFile file) {
        pruneRequested = false;
        FtbqMemberLookup lookup = new FtbqMemberLookup(file);
        long now = System.currentTimeMillis();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        // objects that came back (FTB Quests undo of a delete or move) rejoin their old cluster
        List<QuestCluster> revived = REVIVAL.revive(STORE, lookup, now);
        if (!revived.isEmpty()) {
            dirty = true;
            if (server != null) {
                revived.forEach(c -> ClusterNetwork.sendUpsertToAll(server, c));
            }
        }

        ClusterPruner.Result r = ClusterPruner.prune(STORE, lookup);
        if (r.changed()) {
            REVIVAL.remember(r, now);
            dirty = true;
            if (server != null) {
                r.updated().forEach(c -> ClusterNetwork.sendUpsertToAll(server, c));
                r.removed().forEach(id -> ClusterNetwork.sendRemoveToAll(server, id));
            }
            r.removed().forEach(LAST_BOUNDS::remove);
        }
        followMembers(file, server);
    }

    /**
     * Members moved with FTB Quests' own tools (multi-select move, arrow keys, its undo): if all of a cluster's
     * members moved together, move the box by the same amount so it stays with them.
     */
    private static void followMembers(ServerQuestFile file, MinecraftServer server) {
        for (QuestCluster c : STORE.all()) {
            double[] b = memberBounds(file, c);
            if (b == null) {
                continue;
            }
            double[] prev = LAST_BOUNDS.put(c.id(), b);
            if (SKIP_FOLLOW.remove(c.id()) || prev == null) {
                continue;
            }
            boolean sameShape = Math.abs(b[2] - prev[2]) < 1e-6 && Math.abs(b[3] - prev[3]) < 1e-6;
            double dx = b[0] - prev[0], dy = b[1] - prev[1];
            if (sameShape && (Math.abs(dx) > 1e-6 || Math.abs(dy) > 1e-6)) {
                QuestCluster moved = c.withBoxPosition(c.x() + dx, c.y() + dy);
                STORE.put(moved);
                dirty = true;
                if (server != null) {
                    ClusterNetwork.sendUpsertToAll(server, moved);
                }
            }
        }
        LAST_BOUNDS.keySet().removeIf(id -> !STORE.contains(id));
    }

    private static double[] memberBounds(ServerQuestFile file, QuestCluster c) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean any = false;
        for (long id : c.members()) {
            if (file.getBase(id) instanceof Movable m) {
                any = true;
                minX = Math.min(minX, m.getX() - m.getWidth() / 2D);
                maxX = Math.max(maxX, m.getX() + m.getWidth() / 2D);
                minY = Math.min(minY, m.getY() - m.getHeight() / 2D);
                maxY = Math.max(maxY, m.getY() + m.getHeight() / 2D);
            }
        }
        return any ? new double[]{(minX + maxX) / 2D, (minY + maxY) / 2D, maxX - minX, maxY - minY} : null;
    }

    /** Sets a cluster to the given state (null = removed) without validation; used by undo/redo. */
    static void setRaw(ServerQuestFile file, long id, QuestCluster state) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        SKIP_FOLLOW.add(id);
        if (state == null) {
            if (STORE.remove(id).isPresent() && server != null) {
                ClusterNetwork.sendRemoveToAll(server, id);
            }
        } else {
            STORE.put(state);
            if (server != null) {
                ClusterNetwork.sendUpsertToAll(server, state);
            }
        }
        dirty = true;
        file.markDirty(); // also schedules a prune, which drops members that no longer exist
    }

    // ---------------------------------------------------------------- edits requested by clients

    public static void handleCreate(ServerPlayer player, long chapterId, String title, List<Long> members) {
        withEditableFile(player, file -> applyWithHistory(player, file, draft(file, chapterId, title, members)));
    }

    public static void handleUpdate(ServerPlayer player, QuestCluster cluster) {
        withEditableFile(player, file -> {
            if (!STORE.contains(cluster.id())) {
                reject(player, new ValidationError("no_cluster"));
                return;
            }
            applyWithHistory(player, file, cluster);
        });
    }

    /** Removes the cluster only; member quests are kept where they are. Undoable. */
    public static void handleDissolve(ServerPlayer player, long clusterId) {
        withEditableFile(player, file -> {
            Optional<QuestCluster> current = STORE.get(clusterId);
            if (current.isEmpty()) {
                reject(player, new ValidationError("no_cluster"));
                return;
            }
            file.getHistoryStack().addAndApply(file, new ClusterChangeEvent(current.get(), null));
        });
    }

    /** Moves box and members by (dx, dy) as one undo step. */
    public static void handleMove(ServerPlayer player, long clusterId, double dx, double dy) {
        withEditableFile(player, file -> {
            Optional<QuestCluster> current = STORE.get(clusterId);
            if (current.isEmpty() || !Double.isFinite(dx) || !Double.isFinite(dy) || Math.abs(dx) > 10_000 || Math.abs(dy) > 10_000) {
                reject(player, new ValidationError(current.isEmpty() ? "no_cluster" : "bad_position"));
                return;
            }
            QuestCluster c = current.get();
            dev.ftb.mods.ftbquests.quest.Chapter chapter = file.getChapter(c.chapterId());
            QuestCluster moved = c.withBoxPosition(c.x() + dx, c.y() + dy);
            Optional<ValidationError> err = ClusterValidator.validate(moved, STORE, new FtbqMemberLookup(file));
            if (err.isPresent() || chapter == null) {
                reject(player, err.orElse(new ValidationError("no_chapter", QuestCluster.idToString(c.chapterId()))));
                return;
            }
            List<dev.ftb.mods.ftbquests.quest.history.QuestBookEditEvent> events = new ArrayList<>();
            for (long id : c.members()) {
                if (file.getBase(id) instanceof Movable m && !m.isPositionLocked()) {
                    dev.ftb.mods.ftbquests.quest.history.events.MoveMovableObject
                            .create(m, chapter, m.getX() + dx, m.getY() + dy).ifPresent(events::add);
                }
            }
            // Quest Enhance anchor points that only serve lines inside the cluster move with it
            for (long imageId : com.questclusters.compat.QuestEnhanceCompat.anchorsInside(chapter, c.members())) {
                if (!c.members().contains(imageId) && file.getBase(imageId) instanceof Movable m && !m.isPositionLocked()) {
                    dev.ftb.mods.ftbquests.quest.history.events.MoveMovableObject
                            .create(m, chapter, m.getX() + dx, m.getY() + dy).ifPresent(events::add);
                }
            }
            events.add(new ClusterChangeEvent(c, moved));
            file.getHistoryStack().addAndApply(file, new dev.ftb.mods.ftbquests.quest.history.events.CompositeEditEvent(events));
        });
    }

    /** Validates, then records the change in FTB Quests' history (which applies it). */
    private static void applyWithHistory(ServerPlayer player, ServerQuestFile file, QuestCluster cluster) {
        Optional<ValidationError> err = ClusterValidator.validate(cluster, STORE, new FtbqMemberLookup(file));
        if (err.isPresent()) {
            reject(player, err.get());
            return;
        }
        file.getHistoryStack().addAndApply(file, new ClusterChangeEvent(STORE.get(cluster.id()).orElse(null), cluster));
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
                // separate step after Quest Enhance's paste steps; undo it first, then the pasted quests
                applyWithHistory(player, file, new QuestCluster(newId(), chapterId, t.title(), t.subtitle(), t.color(),
                        t.defaultExpanded(), t.x(), t.y(), t.width(), t.height(), members, t.style()));
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

    /** Validates, stores, saves and broadcasts. Returns the reason if rejected. */
    public static Optional<ValidationError> applyChecked(ServerQuestFile file, QuestCluster cluster) {
        Optional<ValidationError> err = ClusterValidator.validate(cluster, STORE, new FtbqMemberLookup(file));
        if (err.isPresent()) {
            return err;
        }
        STORE.put(cluster);
        SKIP_FOLLOW.add(cluster.id());
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
        return c.withBoxPosition((minX + maxX) / 2D, (minY + maxY) / 2D).withBoxSize(3D, 1D);
    }
}
