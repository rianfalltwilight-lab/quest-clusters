package com.questclusters.migration;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class MigrationRunnerTest {
    static Migration m(String id, boolean needed, Runnable body, List<String> calls) {
        return new Migration() {
            public String id() { return id; }
            public String description() { return id; }
            public boolean isNeeded(MigrationContext ctx) { return needed; }
            public void run(MigrationContext ctx) { calls.add(id); body.run(); }
        };
    }

    @Test
    void runsOnceWithBackupAndRetriesFailures(@TempDir Path tmp) throws Exception {
        Path quests = tmp.resolve("config/ftbquests/quests");
        Files.createDirectories(quests.resolve("chapters"));
        Files.writeString(quests.resolve("chapters/a.snbt"), "{ id: \"01\", quest_enhance_decorative_lines: [] }");
        Files.writeString(quests.resolve("data.snbt"), "{ version: 13 }");
        List<String> calls = new ArrayList<>();
        boolean[] failNow = {true};
        var ms = List.of(
                m("a", true, () -> {}, calls),
                m("b", true, () -> { if (failNow[0]) throw new IllegalStateException("boom"); }, calls),
                m("c", false, () -> {}, calls));

        var r1 = MigrationRunner.run(new MigrationContext(quests, tmp.resolve("config"), null), ms, 1_000L);
        assertEquals(List.of("a"), r1.applied());
        assertEquals(List.of("b"), r1.failed());
        assertEquals(List.of("c"), r1.skipped(), "not needed: not run, not recorded");
        assertNotNull(r1.backup());
        try (ZipFile z = new ZipFile(r1.backup().toFile())) {
            assertNotNull(z.getEntry("chapters/a.snbt"));
            assertNotNull(z.getEntry("data.snbt"));
        }
        assertTrue(MigrationRunner.applied(quests).containsKey("a"));

        failNow[0] = false;
        var r2 = MigrationRunner.run(new MigrationContext(quests, tmp.resolve("config"), null), ms, 2_000L);
        assertEquals(List.of("b"), r2.applied(), "failed one retried, applied one not repeated");
        assertEquals(List.of("a", "b", "b"), calls);

        var r3 = MigrationRunner.run(new MigrationContext(quests, tmp.resolve("config"), null), ms, 3_000L);
        assertFalse(r3.didSomething());
        assertNull(r3.backup(), "no backup when nothing is pending");
    }

    @Test
    void rawChapterKeysAreVisible(@TempDir Path tmp) throws Exception {
        Path quests = tmp.resolve("quests");
        Files.createDirectories(quests.resolve("chapters"));
        Files.writeString(quests.resolve("chapters/x.snbt"), "{ id: \"02\", quest_enhance_hidden_dependency_lines: [] }");
        var ctx = new MigrationContext(quests, tmp, null);
        assertTrue(ctx.anyChapterHasKey("quest_enhance_"));
        assertFalse(ctx.anyChapterHasKey("certain_"));
        assertEquals(new CompoundTag(), new MigrationContext(tmp.resolve("missing"), tmp, null).rawFileData());
    }

    @Test
    void keepsOnlyNewestBackups(@TempDir Path tmp) throws Exception {
        Path quests = tmp.resolve("quests");
        Files.createDirectories(quests);
        Files.writeString(quests.resolve("data.snbt"), "{}");
        for (int i = 0; i < MigrationRunner.KEEP_BACKUPS + 3; i++) {
            MigrationRunner.backup(quests, 1_000_000L * (i + 1), "t");
        }
        try (var s = Files.list(MigrationRunner.backupFolder(quests))) {
            assertEquals(MigrationRunner.KEEP_BACKUPS, s.count());
        }
    }

    @Test
    void duplicateIdsRejected() {
        MigrationRegistry.register(m("unique-test-id", false, () -> {}, new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> MigrationRegistry.register(m("unique-test-id", false, () -> {}, new ArrayList<>())));
    }
}
