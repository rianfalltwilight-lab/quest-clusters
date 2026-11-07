package com.questclusters.editor.server;

import com.questclusters.editor.QuestClustersEditor;
import com.questclusters.editor.data.BookExtras;
import com.questclusters.editor.data.ExtrasNbt;
import com.questclusters.editor.data.NodeKeys;
import com.questclusters.editor.data.QuestExtras;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.nbt.CompoundTag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongUnaryOperator;
import java.util.stream.Stream;

/**
 * {@code config/ftbquests/quests/quest_clusters/editor/}: {@code book.snbt} plus one file per chapter. Kept out of
 * FTB Quests' own files, so FTB Quests re-saving a chapter never drops it and removing the module loses nothing else.
 */
public final class EditorStorage {
    public static final String FOLDER = "quest_clusters/editor";
    public static final String BOOK_FILE = "book.snbt";

    private EditorStorage() {
    }

    public static Path folder(Path questsFolder) {
        return questsFolder.resolve(FOLDER);
    }

    public static BookExtras load(Path questsFolder) {
        BookExtras b = new BookExtras();
        Path dir = folder(questsFolder);
        if (!Files.isDirectory(dir)) {
            return b;
        }
        Path book = dir.resolve(BOOK_FILE);
        if (Files.exists(book)) {
            CompoundTag t = SNBT.read(book);
            if (t != null) ExtrasNbt.readBook(t, b);
            else QuestClustersEditor.LOGGER.error("Could not parse {}", book);
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".snbt") && !p.getFileName().toString().equals(BOOK_FILE))
                    .sorted().forEach(p -> {
                        CompoundTag t = SNBT.read(p);
                        if (t == null || ExtrasNbt.readChapter(t, b) == null) {
                            QuestClustersEditor.LOGGER.error("Could not parse editor file {}", p);
                        }
                    });
        } catch (IOException e) {
            QuestClustersEditor.LOGGER.error("Failed to list {}", dir, e);
        }
        return b;
    }

    /**
     * @param chapterOf current chapter id of a quest or image id (0 if unknown: the data is then dropped,
     *                  the object no longer exists)
     */
    public static void save(Path questsFolder, BookExtras b, LongUnaryOperator chapterOf) {
        Path dir = folder(questsFolder);
        Map<Long, Map<Long, QuestExtras>> quests = new LinkedHashMap<>();
        Map<Long, List<Long>> images = new LinkedHashMap<>();
        b.quests.forEach((id, q) -> {
            long ch = chapterOf.applyAsLong(id);
            if (ch != 0L) quests.computeIfAbsent(ch, k -> new LinkedHashMap<>()).put(id, q);
        });
        for (long id : b.excludedImages) {
            long ch = chapterOf.applyAsLong(id);
            if (ch != 0L) images.computeIfAbsent(ch, k -> new ArrayList<>()).add(id);
        }
        Set<Long> chapters = new java.util.LinkedHashSet<>(b.chapters.keySet());
        chapters.addAll(quests.keySet());
        chapters.addAll(images.keySet());

        Set<String> written = new HashSet<>();
        try {
            Files.createDirectories(dir);
            if (!b.sidebarBackground.isEmpty()) {
                SNBT.write(dir.resolve(BOOK_FILE), (SNBTCompoundTag) ExtrasNbt.writeBook(b, SNBTCompoundTag::new));
                written.add(BOOK_FILE);
            }
            for (long ch : chapters) {
                CompoundTag t = ExtrasNbt.writeChapter(ch, b.chapters.get(ch), quests.getOrDefault(ch, Map.of()),
                        images.getOrDefault(ch, List.of()), SNBTCompoundTag::new);
                if (t == null) continue;
                String name = NodeKeys.hex(ch) + ".snbt";
                if (!SNBT.write(dir.resolve(name), (SNBTCompoundTag) t)) {
                    QuestClustersEditor.LOGGER.error("Failed to write editor file {}", name);
                }
                written.add(name);
            }
            try (Stream<Path> files = Files.list(dir)) {
                for (Path p : files.toList()) {
                    String n = p.getFileName().toString();
                    if (n.endsWith(".snbt") && !written.contains(n)) Files.deleteIfExists(p);
                }
            }
        } catch (IOException e) {
            QuestClustersEditor.LOGGER.error("Failed to save editor data to {}", dir, e);
        }
    }
}
