package com.questclusters.data;

import java.util.List;

/**
 * One quest cluster: a set of movable quest objects (quests, quest links, chapter images) inside a single chapter
 * that can be collapsed into a single labelled box.
 * <p>
 * Coordinates and sizes are in FTB Quests canvas units (the same units as {@code Quest#getX()}).
 * The box fields describe where the collapsed box is drawn; members keep their own positions.
 *
 * @param id              unique cluster id (never 0)
 * @param chapterId       FTB Quests id of the chapter this cluster belongs to
 * @param title           text shown in the box
 * @param subtitle        optional second line, may be empty
 * @param color           ARGB colour of the box
 * @param defaultExpanded whether players see the cluster expanded until they toggle it themselves
 * @param x               centre X of the collapsed box
 * @param y               centre Y of the collapsed box
 * @param width           width of the collapsed box
 * @param height          height of the collapsed box
 * @param members         FTB Quests ids of the member objects, in insertion order, no duplicates
 */
public record QuestCluster(
        long id,
        long chapterId,
        String title,
        String subtitle,
        int color,
        boolean defaultExpanded,
        double x,
        double y,
        double width,
        double height,
        List<Long> members
) {
    public static final int MAX_TITLE_LENGTH = 128;
    public static final int MAX_SUBTITLE_LENGTH = 256;
    public static final int MAX_MEMBERS = 512;
    public static final double MIN_BOX_SIZE = 0.5D;
    public static final double MAX_BOX_SIZE = 64D;
    public static final double MAX_COORDINATE = 1_000_000D;
    public static final int DEFAULT_COLOR = 0xFF3B4252;

    public QuestCluster {
        title = title == null ? "" : title;
        subtitle = subtitle == null ? "" : subtitle;
        members = members == null ? List.of() : List.copyOf(members);
    }

    public QuestCluster withMembers(List<Long> newMembers) {
        return new QuestCluster(id, chapterId, title, subtitle, color, defaultExpanded, x, y, width, height, newMembers);
    }

    public QuestCluster withId(long newId) {
        return new QuestCluster(newId, chapterId, title, subtitle, color, defaultExpanded, x, y, width, height, members);
    }

    /** Formats an id the same way FTB Quests does in its snbt files (16 upper-case hex digits). */
    public static String idToString(long id) {
        return String.format("%016X", id);
    }

    /** Parses an id written by {@link #idToString(long)}; also accepts an optional leading '#'. Returns 0 on failure. */
    public static long idFromString(String s) {
        if (s == null) {
            return 0L;
        }
        String t = s.startsWith("#") ? s.substring(1) : s;
        if (t.isEmpty() || t.length() > 16) {
            return 0L;
        }
        try {
            return Long.parseUnsignedLong(t, 16);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
