package com.questclusters.editor.data;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * All editor data of one quest book. Chapter data is keyed by chapter id; quest and image data by object id
 * (independent of the chapter, so moving a quest keeps its backgrounds).
 */
public final class BookExtras {
    public String sidebarBackground = "";
    public final Map<Long, ChapterExtras> chapters = new LinkedHashMap<>();
    public final Map<Long, QuestExtras> quests = new LinkedHashMap<>();
    /** Chapter image ids excluded from drag-box selection. */
    public final Set<Long> excludedImages = new HashSet<>();

    public ChapterExtras chapter(long id) {
        ChapterExtras c = chapters.get(id);
        return c == null ? EMPTY_CHAPTER : c;
    }

    public ChapterExtras editChapter(long id) {
        return chapters.computeIfAbsent(id, k -> new ChapterExtras());
    }

    public QuestExtras quest(long id) {
        return quests.getOrDefault(id, QuestExtras.EMPTY);
    }

    public void setQuest(long id, QuestExtras q) {
        if (q == null || q.isEmpty()) {
            quests.remove(id);
        } else {
            quests.put(id, q);
        }
    }

    public boolean isEmpty() {
        return sidebarBackground.isEmpty() && chapters.values().stream().allMatch(ChapterExtras::isEmpty)
                && quests.isEmpty() && excludedImages.isEmpty();
    }

    private static final ChapterExtras EMPTY_CHAPTER = new ChapterExtras();
}
