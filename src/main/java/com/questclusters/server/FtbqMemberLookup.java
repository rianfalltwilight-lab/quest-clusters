package com.questclusters.server;

import com.questclusters.data.MemberLookup;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Movable;

import java.util.OptionalLong;

/** {@link MemberLookup} backed by a live FTB Quests file (server or client). */
public record FtbqMemberLookup(BaseQuestFile file) implements MemberLookup {
    @Override
    public boolean chapterExists(long chapterId) {
        return file.getChapter(chapterId) != null;
    }

    @Override
    public OptionalLong chapterOfMovable(long objectId) {
        if (file.getBase(objectId) instanceof Movable movable) {
            Chapter chapter = movable.getChapter();
            if (chapter != null) {
                return OptionalLong.of(chapter.id);
            }
        }
        return OptionalLong.empty();
    }
}
