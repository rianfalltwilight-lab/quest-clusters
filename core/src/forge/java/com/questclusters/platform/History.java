package com.questclusters.platform;

import dev.ftb.mods.ftbquests.net.MoveMovableResponseMessage;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.network.chat.Component;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Optional;

/**
 * Edits of the quest book made by the mods. On 1.21.1 they go into FTB Quests' undo/redo history; FTB Quests for
 * 1.20.1 has none, so here they are applied and kept in a short undo/redo list of their own (server side, not
 * saved; FTB Quests' own edits are not in it).
 */
public final class History {
    /** One undoable change. */
    public interface Change {
        boolean apply(ServerQuestFile file);

        boolean applyUndo(ServerQuestFile file);

        List<Component> description(BaseQuestFile file);
    }

    private History() {
    }

    private static final int LIMIT = 50;
    private static final java.util.Deque<List<? extends Change>> UNDO = new java.util.ArrayDeque<>();
    private static final java.util.Deque<List<? extends Change>> REDO = new java.util.ArrayDeque<>();

    public static void record(ServerQuestFile file, List<? extends Change> changes) {
        if (changes.isEmpty()) return;
        boolean changed = false;
        for (Change c : changes) changed |= c.apply(file);
        if (changed) file.markDirty();
        UNDO.push(List.copyOf(changes));
        while (UNDO.size() > LIMIT) UNDO.removeLast();
        REDO.clear();
    }

    /** Undoes the last recorded change; false if there is none. */
    public static boolean undo(ServerQuestFile file) {
        List<? extends Change> last = UNDO.poll();
        if (last == null) return false;
        for (int i = last.size() - 1; i >= 0; i--) last.get(i).applyUndo(file);
        file.markDirty();
        REDO.push(last);
        return true;
    }

    public static boolean redo(ServerQuestFile file) {
        List<? extends Change> next = REDO.poll();
        if (next == null) return false;
        for (Change c : next) c.apply(file);
        file.markDirty();
        UNDO.push(next);
        return true;
    }

    /** The quest book was reloaded or the server stopped: old entries would point at stale objects. */
    public static void clear() {
        UNDO.clear();
        REDO.clear();
    }

    public static void record(ServerQuestFile file, Change change) {
        record(file, List.of(change));
    }

    /** Moving a quest, link or image, as FTB Quests' own move packet does it (empty if it would not move). */
    public static Optional<Change> move(Movable m, Chapter chapter, double x, double y) {
        if (m.getChapter() == chapter && m.getX() == x && m.getY() == y) return Optional.empty();
        double oldX = m.getX(), oldY = m.getY();
        long oldChapter = m.getChapter().id;
        return Optional.of(new Change() {
            @Override
            public boolean apply(ServerQuestFile file) {
                return moveTo(file, m, chapter.id, x, y);
            }

            @Override
            public boolean applyUndo(ServerQuestFile file) {
                return moveTo(file, m, oldChapter, oldX, oldY);
            }

            @Override
            public List<Component> description(BaseQuestFile file) {
                return List.of(m.getTitle());
            }
        });
    }

    private static boolean moveTo(ServerQuestFile file, Movable m, long chapterId, double x, double y) {
        m.onMoved(x, y, chapterId);
        file.markDirty();
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) new MoveMovableResponseMessage(m, chapterId, x, y).sendToAll(server);
        return true;
    }

    /** FTB Quests for 1.20.1 has no position lock. */
    public static boolean locked(Movable m) {
        return false;
    }

    public static Component logText(Component c) {
        return c;
    }
}
