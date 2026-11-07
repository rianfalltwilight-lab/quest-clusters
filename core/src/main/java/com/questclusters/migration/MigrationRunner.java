package com.questclusters.migration;

import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Runs pending migrations once. Before the first pending one runs, the whole quest book folder is zipped to
 * {@code config/ftbquests/quest_clusters_backups/}. State lives in {@code quests/quest_clusters/migrations.snbt}.
 */
public final class MigrationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger("QuestClusters/Migration");
    public static final String STATE_FILE = "migrations.snbt";
    public static final String BACKUP_FOLDER = "quest_clusters_backups";
    public static final int KEEP_BACKUPS = 10;

    /** Outcome of one run, for logs, tests and the status command. */
    public record Result(List<String> applied, List<String> failed, List<String> skipped, Path backup, List<String> report) {
        public boolean didSomething() {
            return !applied.isEmpty() || !failed.isEmpty();
        }
    }

    private MigrationRunner() {
    }

    public static Path statePath(Path questsFolder) {
        return questsFolder.resolve("quest_clusters").resolve(STATE_FILE);
    }

    public static Path backupFolder(Path questsFolder) {
        return questsFolder.getParent().resolve(BACKUP_FOLDER);
    }

    /** Applied migration id → when (ms). */
    public static Map<String, Long> applied(Path questsFolder) {
        Map<String, Long> out = new LinkedHashMap<>();
        Path p = statePath(questsFolder);
        if (Files.exists(p)) {
            CompoundTag t = SNBT.read(p);
            if (t != null) {
                ListTag l = t.getList("applied", Tag.TAG_COMPOUND);
                for (int i = 0; i < l.size(); i++) {
                    out.put(l.getCompound(i).getString("id"), l.getCompound(i).getLong("time"));
                }
            }
        }
        return out;
    }

    public static Result run(MigrationContext ctx, List<Migration> migrations, long now) {
        Map<String, Long> applied = applied(ctx.questsFolder());
        List<Migration> pending = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Migration m : migrations) {
            if (applied.containsKey(m.id())) {
                continue;
            }
            try {
                if (m.isNeeded(ctx)) {
                    pending.add(m);
                } else {
                    skipped.add(m.id());
                }
            } catch (Exception e) {
                LOGGER.error("Migration {} could not check its data; skipped this start", m.id(), e);
                skipped.add(m.id());
            }
        }
        if (pending.isEmpty()) {
            return new Result(List.of(), List.of(), skipped, null, ctx.report());
        }

        Path backup;
        try {
            backup = backup(ctx.questsFolder(), now, "before-migration");
            ctx.log("Backup: " + backup);
        } catch (IOException e) {
            // never migrate without a backup
            LOGGER.error("Could not back up {}; migrations postponed", ctx.questsFolder(), e);
            return new Result(List.of(), pending.stream().map(Migration::id).toList(), skipped, null, ctx.report());
        }

        List<String> ok = new ArrayList<>(), failed = new ArrayList<>();
        for (Migration m : pending) {
            try {
                LOGGER.info("Running migration {}: {}", m.id(), m.description());
                m.run(ctx);
                applied.put(m.id(), now);
                ok.add(m.id());
            } catch (Exception e) {
                LOGGER.error("Migration {} failed; the backup {} has the data from before", m.id(), backup, e);
                ctx.log("FAILED " + m.id() + ": " + e);
                failed.add(m.id());
            }
        }
        writeState(ctx.questsFolder(), applied);
        ctx.report().forEach(l -> LOGGER.info("  {}", l));
        return new Result(ok, failed, skipped, backup, ctx.report());
    }

    /** Zips the quest book folder; keeps the newest {@link #KEEP_BACKUPS}. */
    public static Path backup(Path questsFolder, long now, String reason) throws IOException {
        Path dir = backupFolder(questsFolder);
        Files.createDirectories(dir);
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(now));
        Path zip = dir.resolve("quests-" + stamp + "-" + reason + ".zip");
        for (int i = 2; Files.exists(zip); i++) {
            zip = dir.resolve("quests-" + stamp + "-" + reason + "-" + i + ".zip");
        }
        try (OutputStream os = Files.newOutputStream(zip); ZipOutputStream zos = new ZipOutputStream(os);
             Stream<Path> files = Files.walk(questsFolder)) {
            for (Path f : files.filter(Files::isRegularFile).sorted().toList()) {
                zos.putNextEntry(new ZipEntry(questsFolder.relativize(f).toString().replace('\\', '/')));
                Files.copy(f, zos);
                zos.closeEntry();
            }
        }
        try (Stream<Path> old = Files.list(dir)) {
            List<Path> zips = old.filter(p -> p.getFileName().toString().endsWith(".zip"))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed()).toList();
            for (int i = KEEP_BACKUPS; i < zips.size(); i++) {
                Files.deleteIfExists(zips.get(i));
            }
        }
        return zip;
    }

    private static void writeState(Path questsFolder, Map<String, Long> applied) {
        SNBTCompoundTag root = new SNBTCompoundTag();
        root.putInt("version", 1);
        ListTag list = new ListTag();
        applied.forEach((id, time) -> {
            SNBTCompoundTag t = new SNBTCompoundTag();
            t.putString("id", id);
            t.putLong("time", time);
            list.add(t);
        });
        root.put("applied", list);
        try {
            Files.createDirectories(statePath(questsFolder).getParent());
            SNBT.write(statePath(questsFolder), root);
        } catch (IOException e) {
            LOGGER.error("Could not write migration state", e);
        }
    }
}
