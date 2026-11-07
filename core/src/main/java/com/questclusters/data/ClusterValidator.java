package com.questclusters.data;

import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Checks a cluster that a client wants to create or update, against the current quest file and store.
 */
public final class ClusterValidator {
    private ClusterValidator() {
    }

    public static Optional<ValidationError> validate(QuestCluster c, ClusterStore store, MemberLookup lookup) {
        if (c.id() == 0L) {
            return error("bad_id");
        }
        if (!lookup.chapterExists(c.chapterId())) {
            return error("no_chapter", QuestCluster.idToString(c.chapterId()));
        }
        if (c.title().isBlank()) {
            return error("empty_title");
        }
        if (c.title().length() > QuestCluster.MAX_TITLE_LENGTH) {
            return error("title_too_long", QuestCluster.MAX_TITLE_LENGTH);
        }
        if (c.subtitle().length() > QuestCluster.MAX_SUBTITLE_LENGTH) {
            return error("subtitle_too_long", QuestCluster.MAX_SUBTITLE_LENGTH);
        }
        if (!isCoordinate(c.x()) || !isCoordinate(c.y())) {
            return error("bad_position");
        }
        if (!isBoxSize(c.width()) || !isBoxSize(c.height())) {
            return error("bad_size", QuestCluster.MIN_BOX_SIZE, QuestCluster.MAX_BOX_SIZE);
        }
        String bg = c.style().background();
        if (bg.length() > ClusterStyle.MAX_BACKGROUND_LENGTH || !bg.matches("([a-z0-9_.-]+:)?[a-z0-9_./-]*")) {
            return error("bad_background");
        }
        if (c.members().isEmpty()) {
            return error("no_members");
        }
        if (c.members().size() > QuestCluster.MAX_MEMBERS) {
            return error("too_many_members", QuestCluster.MAX_MEMBERS);
        }

        Set<Long> seen = new HashSet<>();
        for (long member : c.members()) {
            String memberStr = QuestCluster.idToString(member);
            if (!seen.add(member)) {
                return error("duplicate_member", memberStr);
            }
            OptionalLong chapter = lookup.chapterOfMovable(member);
            if (chapter.isEmpty()) {
                return error("no_member", memberStr);
            }
            if (chapter.getAsLong() != c.chapterId()) {
                return error("member_other_chapter", memberStr);
            }
            Optional<QuestCluster> owner = store.clusterOfMember(member);
            if (owner.isPresent() && owner.get().id() != c.id()) {
                return error("member_in_other_cluster", memberStr, owner.get().title());
            }
        }

        Optional<QuestCluster> existing = store.get(c.id());
        if (existing.isPresent() && existing.get().chapterId() != c.chapterId()) {
            return error("chapter_changed");
        }
        return Optional.empty();
    }

    private static boolean isCoordinate(double d) {
        return Double.isFinite(d) && Math.abs(d) <= QuestCluster.MAX_COORDINATE;
    }

    private static boolean isBoxSize(double d) {
        return Double.isFinite(d) && d >= QuestCluster.MIN_BOX_SIZE && d <= QuestCluster.MAX_BOX_SIZE;
    }

    private static Optional<ValidationError> error(String key, Object... args) {
        return Optional.of(new ValidationError(key, args));
    }
}
