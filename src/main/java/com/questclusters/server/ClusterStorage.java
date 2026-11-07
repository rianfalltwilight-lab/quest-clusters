package com.questclusters.server;

import com.questclusters.QuestClusters;
import com.questclusters.data.ClusterNbt;
import com.questclusters.data.QuestCluster;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reads and writes {@code config/ftbquests/quests/quest_clusters/<chapter id>.snbt}.
 * Kept outside FTB Quests' own chapter files so that removing this mod never corrupts or loses quest data,
 * and so FTB Quests re-saving a chapter never drops cluster data.
 */
public final class ClusterStorage {
    public static final String FOLDER_NAME = "quest_clusters";
    private static final String EXT = ".snbt";

    private ClusterStorage() {
    }

    public static Path folder(Path questsFolder) {
        return questsFolder.resolve(FOLDER_NAME);
    }

    public static List<QuestCluster> load(Path questsFolder) {
        Path dir = folder(questsFolder);
        List<QuestCluster> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(EXT)).sorted().forEach(p -> {
                try {
                    SNBTCompoundTag tag = SNBT.read(p);
                    if (tag == null) {
                        QuestClusters.LOGGER.error("Could not parse quest cluster file {}", p);
                        return;
                    }
                    out.addAll(ClusterNbt.readChapterFile(tag));
                } catch (Exception e) {
                    QuestClusters.LOGGER.error("Failed to read quest cluster file {}", p, e);
                }
            });
        } catch (IOException e) {
            QuestClusters.LOGGER.error("Failed to list {}", dir, e);
        }
        return out;
    }

    public static void save(Path questsFolder, Collection<QuestCluster> clusters) {
        Path dir = folder(questsFolder);
        Map<Long, List<QuestCluster>> byChapter = new LinkedHashMap<>();
        for (QuestCluster c : clusters) {
            byChapter.computeIfAbsent(c.chapterId(), k -> new ArrayList<>()).add(c);
        }

        Set<String> written = new HashSet<>();
        try {
            if (!byChapter.isEmpty()) {
                Files.createDirectories(dir);
            }
            for (Map.Entry<Long, List<QuestCluster>> e : byChapter.entrySet()) {
                String name = QuestCluster.idToString(e.getKey()) + EXT;
                SNBTCompoundTag tag = (SNBTCompoundTag) ClusterNbt.writeChapterFile(e.getKey(), e.getValue(), SNBTCompoundTag::new);
                if (!SNBT.write(dir.resolve(name), tag)) {
                    QuestClusters.LOGGER.error("Failed to write quest cluster file {}", name);
                }
                written.add(name);
            }

            // chapters that no longer have clusters: remove their stale files (only our own *.snbt files)
            if (Files.isDirectory(dir)) {
                try (Stream<Path> files = Files.list(dir)) {
                    for (Path p : files.toList()) {
                        String n = p.getFileName().toString();
                        if (n.endsWith(EXT) && !written.contains(n)
                                && QuestCluster.idFromString(n.substring(0, n.length() - EXT.length())) != 0L) {
                            Files.deleteIfExists(p);
                        }
                    }
                }
            }
        } catch (IOException e) {
            QuestClusters.LOGGER.error("Failed to save quest clusters to {}", dir, e);
        }
    }
}
