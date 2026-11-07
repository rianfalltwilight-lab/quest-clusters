package com.questclusters.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

final class FakeLookup implements MemberLookup {
    final Set<Long> chapters = new HashSet<>();
    final Map<Long, Long> movables = new HashMap<>();

    FakeLookup chapter(long id) {
        chapters.add(id);
        return this;
    }

    FakeLookup movable(long id, long chapter) {
        movables.put(id, chapter);
        return this;
    }

    @Override
    public boolean chapterExists(long chapterId) {
        return chapters.contains(chapterId);
    }

    @Override
    public OptionalLong chapterOfMovable(long objectId) {
        Long c = movables.get(objectId);
        return c == null ? OptionalLong.empty() : OptionalLong.of(c);
    }
}
