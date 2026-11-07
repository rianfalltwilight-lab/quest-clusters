package com.questclusters.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.questclusters.QuestClusters;
import com.questclusters.data.ExpandState;
import com.questclusters.data.QuestCluster;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * This player's expand/collapse choices, remembered per server or singleplayer world in
 * {@code <game dir>/quest_clusters/view_state.json}.
 */
public final class ClusterViewState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ExpandState STATE = new ExpandState();
    private static String worldKey = null;
    private static final Map<Long, Long> LAST_CHANGE = new HashMap<>();

    private ClusterViewState() {
    }

    public static ExpandState state() {
        return STATE;
    }

    public static boolean isExpanded(QuestCluster c) {
        return STATE.isExpanded(c);
    }

    public static boolean isHidden(long memberId) {
        return STATE.isHidden(ClientClusters.store(), memberId);
    }

    private static java.util.Map<Long, Long> anchorOwner = java.util.Map.of();
    private static int anchorRevision = -1;
    private static long anchorTime;

    /**
     * For chapter images: hidden if they are members of a collapsed cluster, or Quest Enhance anchor points that only
     * serve decorative lines inside one.
     */
    public static boolean isHiddenImage(long imageId) {
        if (isHidden(imageId)) {
            return true;
        }
        Long owner = anchorOwners().get(imageId);
        return owner != null && ClientClusters.store().get(owner).map(c -> !STATE.isExpanded(c)).orElse(false);
    }

    private static java.util.Map<Long, Long> anchorOwners() {
        long now = System.currentTimeMillis();
        if (anchorRevision != ClientClusters.revision() || now - anchorTime > 1000L) {
            anchorRevision = ClientClusters.revision();
            anchorTime = now;
            java.util.Map<Long, Long> m = new java.util.HashMap<>();
            var file = dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE;
            if (file != null && com.questclusters.compat.QuestEnhanceCompat.decorativeLinesActive()) {
                for (QuestCluster c : ClientClusters.store().all()) {
                    for (long id : com.questclusters.compat.QuestEnhanceCompat.anchorsInside(file.getChapter(c.chapterId()), c.members())) {
                        m.putIfAbsent(id, c.id());
                    }
                }
            }
            anchorOwner = m;
        }
        return anchorOwner;
    }

    public static void setExpanded(QuestCluster c, boolean expanded) {
        if (STATE.setExpanded(c, expanded)) {
            LAST_CHANGE.put(c.id(), System.currentTimeMillis());
            save();
        }
    }

    /** When this player last expanded or collapsed the cluster (0 = never this session); drives transitions. */
    public static long lastChange(long clusterId) {
        return LAST_CHANGE.getOrDefault(clusterId, 0L);
    }

    public static void toggle(QuestCluster c) {
        setExpanded(c, !STATE.isExpanded(c));
    }

    static void onLogin() {
        worldKey = computeWorldKey();
        Map<Long, Boolean> saved = new HashMap<>();
        JsonObject root = readFile();
        if (worldKey != null && root.has(worldKey) && root.get(worldKey).isJsonObject()) {
            root.getAsJsonObject(worldKey).entrySet().forEach(e -> {
                long id = QuestCluster.idFromString(e.getKey());
                if (id != 0L && e.getValue().isJsonPrimitive()) {
                    saved.put(id, e.getValue().getAsBoolean());
                }
            });
        }
        STATE.load(saved);
    }

    static void onLogout() {
        save();
        STATE.clear();
        worldKey = null;
    }

    private static String computeWorldKey() {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer sp = mc.getSingleplayerServer();
        if (sp != null) {
            Path p = sp.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
            return "world:" + (p == null ? "unknown" : p.toString());
        }
        ServerData sd = mc.getCurrentServer();
        return sd != null ? "server:" + sd.ip.toLowerCase(java.util.Locale.ROOT) : null;
    }

    private static Path file() {
        return FMLPaths.GAMEDIR.get().resolve(QuestClusters.MOD_ID).resolve("view_state.json");
    }

    private static JsonObject readFile() {
        Path f = file();
        if (Files.exists(f)) {
            try {
                var el = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8));
                if (el.isJsonObject()) {
                    return el.getAsJsonObject();
                }
            } catch (Exception e) {
                QuestClusters.LOGGER.warn("Ignoring unreadable {}: {}", f, e.getMessage());
            }
        }
        return new JsonObject();
    }

    private static void save() {
        if (worldKey == null) {
            return;
        }
        JsonObject root = readFile();
        JsonObject mine = new JsonObject();
        STATE.overrides().forEach((id, v) -> mine.addProperty(QuestCluster.idToString(id), v));
        if (mine.isEmpty()) {
            root.remove(worldKey);
        } else {
            root.add(worldKey, mine);
        }
        Path f = file();
        try {
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            QuestClusters.LOGGER.warn("Could not save {}: {}", f, e.getMessage());
        }
    }
}
