package com.questclusters.data;

import java.util.OptionalLong;

/**
 * Read-only view of the quest file, so the cluster logic can be tested without FTB Quests on the classpath.
 */
public interface MemberLookup {
    boolean chapterExists(long chapterId);

    /**
     * @return the chapter id of the movable object (quest, quest link or chapter image) with this id,
     * or empty if there is no such movable object
     */
    OptionalLong chapterOfMovable(long objectId);
}
