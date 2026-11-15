package com.questclusters.editor.perf;

import com.questclusters.editor.QuestClustersEditor;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import net.minecraft.nbt.CompoundTag;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Writes FTB Quests' team progress files off the server thread. The data is still captured on the server thread
 * (FTB Quests serialises it before calling write), so what is written is a consistent snapshot; only turning it into
 * text and the file I/O move to one background thread, which keeps writes in order. Pending writes are waited for
 * before the quest book unloads (server stop).
 */
public final class AsyncTeamSave {
    private static ExecutorService writer;

    private AsyncTeamSave() {
    }

    private static synchronized ExecutorService writer() {
        if (writer == null || writer.isShutdown()) {
            writer = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Quest Clusters team save");
                t.setDaemon(true);
                return t;
            });
        }
        return writer;
    }

    public static void write(Path file, CompoundTag snapshot) {
        writer().execute(() -> {
            if (!SNBT.write(file, snapshot)) {
                QuestClustersEditor.LOGGER.error("Could not write team progress to {}", file);
            }
        });
    }

    /** Blocks until everything queued so far is written (at most 30 s). */
    public static synchronized void flush() {
        if (writer == null) return;
        writer.shutdown();
        try {
            if (!writer.awaitTermination(30, TimeUnit.SECONDS)) {
                QuestClustersEditor.LOGGER.error("Team progress writes did not finish within 30 s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        writer = null;
    }
}
