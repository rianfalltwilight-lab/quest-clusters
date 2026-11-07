package com.questclusters.data;

/**
 * Optional colours of a cluster. A value of 0 means "automatic":
 * title white, subtitle light grey, border follows progress (like quest outlines), frame uses the box colour.
 */
public record ClusterStyle(int titleColor, int subtitleColor, int borderColor, int frameColor) {
    public static final ClusterStyle DEFAULT = new ClusterStyle(0, 0, 0, 0);
    public static final int DEFAULT_TITLE = 0xFFFFFFFF;
    public static final int DEFAULT_SUBTITLE = 0xFFBBBBBB;

    public int titleOr() {
        return titleColor != 0 ? titleColor : DEFAULT_TITLE;
    }

    public int subtitleOr() {
        return subtitleColor != 0 ? subtitleColor : DEFAULT_SUBTITLE;
    }

    public boolean isDefault() {
        return equals(DEFAULT);
    }

    public ClusterStyle withTitle(int c) {
        return new ClusterStyle(c, subtitleColor, borderColor, frameColor);
    }

    public ClusterStyle withSubtitle(int c) {
        return new ClusterStyle(titleColor, c, borderColor, frameColor);
    }

    public ClusterStyle withBorder(int c) {
        return new ClusterStyle(titleColor, subtitleColor, c, frameColor);
    }

    public ClusterStyle withFrame(int c) {
        return new ClusterStyle(titleColor, subtitleColor, borderColor, c);
    }
}
