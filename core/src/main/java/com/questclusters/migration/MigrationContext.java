package com.questclusters.migration;

import dev.ftb.mods.ftblibrary.snbt.SNBT;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * What a migration can look at. Raw files are read straight from disk, so keys written by a removed addon are still
 * visible (FTB Quests ignores unknown keys and would drop them the next time it saves a chapter).
 */
public final class MigrationContext {
    private final Path questsFolder;
    private final Path configFolder;
    @Nullable
    private final Object questFile;
    private final List<String> report = new ArrayList<>();
    private Map<String, CompoundTag> rawChapters;

    /**
     * @param questsFolder {@code config/ftbquests/quests}
     * @param configFolder the game/server {@code config} folder
     * @param questFile    the loaded FTB Quests file (null in unit tests)
     */
    public MigrationContext(Path questsFolder, Path configFolder, @Nullable Object questFile) {
        this.questsFolder = questsFolder;
        this.configFolder = configFolder;
        this.questFile = questFile;
    }

    public Path questsFolder() {
        return questsFolder;
    }

    public Path configFolder() {
        return configFolder;
    }

    /** The loaded {@code ServerQuestFile}, or null in unit tests. */
    @Nullable
    public Object questFile() {
        return questFile;
    }

    public List<String> report() {
        return report;
    }

    public void log(String line) {
        report.add(line);
    }

    /** Every chapter file as raw NBT, keyed by file name without extension. Read once per context. */
    public Map<String, CompoundTag> rawChapters() {
        if (rawChapters == null) {
            rawChapters = readFolder(questsFolder.resolve("chapters"));
        }
        return rawChapters;
    }

    /** {@code data.snbt} as raw NBT (empty if missing). */
    public CompoundTag rawFileData() {
        CompoundTag t = read(questsFolder.resolve("data.snbt"));
        return t == null ? new CompoundTag() : t;
    }

    /** True if any chapter file has a top-level key starting with the prefix. */
    public boolean anyChapterHasKey(String prefix) {
        return rawChapters().values().stream().anyMatch(t -> t.getAllKeys().stream().anyMatch(k -> k.startsWith(prefix)));
    }

    private static Map<String, CompoundTag> readFolder(Path dir) {
        Map<String, CompoundTag> out = new LinkedHashMap<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().endsWith(".snbt")).sorted().forEach(p -> {
                CompoundTag t = read(p);
                if (t != null) {
                    String n = p.getFileName().toString();
                    out.put(n.substring(0, n.length() - 5), t);
                }
            });
        } catch (IOException ignored) {
        }
        return out;
    }

    @Nullable
    private static CompoundTag read(Path p) {
        return Files.exists(p) ? SNBT.read(p) : null;
    }
}
