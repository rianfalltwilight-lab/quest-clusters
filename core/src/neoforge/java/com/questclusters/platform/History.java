package com.questclusters.platform;

import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.history.ChangeType;
import dev.ftb.mods.ftbquests.quest.history.QuestBookEditEvent;
import dev.ftb.mods.ftbquests.quest.history.events.CompositeEditEvent;
import dev.ftb.mods.ftbquests.quest.history.events.MoveMovableObject;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * Edits of the quest book made by the mods. On 1.21.1 they go into FTB Quests' undo/redo history (the arrows in
 * edit mode, Ctrl+Z / Ctrl+Y) and its change log; FTB Quests for 1.20.1 has no history, there they are just applied.
 */
public final class History {
    /** One undoable change. */
    public interface Change {
        boolean apply(ServerQuestFile file);

        boolean applyUndo(ServerQuestFile file);

        List<Component> description(BaseQuestFile file);
    }

    private record Adapter(Change change) implements QuestBookEditEvent {
        @Override
        public boolean apply(ServerQuestFile file) {
            return change.apply(file);
        }

        @Override
        public boolean applyUndo(ServerQuestFile file) {
            return change.applyUndo(file);
        }

        @Override
        public List<Component> description(BaseQuestFile file, ChangeType type) {
            return change.description(file);
        }
    }

    /** FTB Quests' own event, carried through our lists. */
    private record Native(QuestBookEditEvent event) implements Change {
        @Override
        public boolean apply(ServerQuestFile file) {
            return event.apply(file);
        }

        @Override
        public boolean applyUndo(ServerQuestFile file) {
            return event.applyUndo(file);
        }

        @Override
        public List<Component> description(BaseQuestFile file) {
            return event.description(file, ChangeType.INITIAL);
        }
    }

    private History() {
    }

    private static QuestBookEditEvent event(Change c) {
        return c instanceof Native n ? n.event() : new Adapter(c);
    }

    /** Applies the changes as one undo step. */
    public static void record(ServerQuestFile file, List<? extends Change> changes) {
        if (changes.isEmpty()) return;
        QuestBookEditEvent e = changes.size() == 1 ? event(changes.get(0))
                : new CompositeEditEvent(changes.stream().map(History::event).toList());
        file.getHistoryStack().addAndApply(file, e);
    }

    public static void record(ServerQuestFile file, Change change) {
        record(file, List.of(change));
    }

    /** Moving a quest, link or image (empty if it would not move). */
    public static Optional<Change> move(Movable m, Chapter chapter, double x, double y) {
        return MoveMovableObject.create(m, chapter, x, y).map(Native::new);
    }

    public static boolean locked(Movable m) {
        return m.isPositionLocked();
    }

    /** Text shown in the change log: without click/hover events. */
    public static Component logText(Component c) {
        return QuestBookEditEvent.sanitizeComponent(c);
    }

    /** FTB Quests clears its own history; nothing to do here. */
    public static void clear() {
    }
}
