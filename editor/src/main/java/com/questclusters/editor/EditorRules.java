package com.questclusters.editor;

import com.questclusters.editor.data.ChapterExtras;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;

/** Gameplay rules shared by server and client. */
public final class EditorRules {
    private EditorRules() {
    }

    public static boolean prerequisitesMet(Chapter chapter, TeamData data) {
        ChapterExtras c = EditorLineData.bookFor(chapter).chapter(chapter.id);
        if (c.prerequisites.isEmpty() || chapter.getQuestFile().canEdit() || data == null) {
            return true;
        }
        for (long id : c.prerequisites) {
            // a missing quest or one inside the chapter itself never counts as done (same as Quest Enhance)
            if (!(chapter.getQuestFile().getBase(id) instanceof Quest q) || q.getChapter() == chapter || !data.isCompleted(q)) {
                return false;
            }
        }
        return true;
    }
}
