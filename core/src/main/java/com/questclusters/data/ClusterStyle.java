package com.questclusters.data;

/**
 * Optional look of a cluster. Colour value 0 means "automatic": title white, subtitle light grey, border follows
 * progress (like quest outlines), frame uses the box colour.
 *
 * @param background     texture of the box background ("" = plain colour), e.g. "minecraft:textures/block/stone.png"
 * @param backgroundMode how the texture fills the box: 0 stretch, 1 tile, 2 nine-slice
 * @param sliceBorder    nine-slice border width in texture pixels
 * @param backgroundOnFrame also draw the texture behind the expanded frame
 * @param freeBox        the collapsed box keeps its own position: dragging it leaves the members where they are,
 *                       and moving the members (frame drag, FTB Quests' tools) leaves the box
 */
public record ClusterStyle(int titleColor, int subtitleColor, int borderColor, int frameColor,
                           String background, int backgroundMode, int sliceBorder, boolean backgroundOnFrame, boolean freeBox) {
    public static final ClusterStyle DEFAULT = new ClusterStyle(0, 0, 0, 0, "", 0, 4, false, false);
    public static final int DEFAULT_TITLE = 0xFFFFFFFF;
    public static final int DEFAULT_SUBTITLE = 0xFFBBBBBB;
    public static final int MODE_STRETCH = 0, MODE_TILE = 1, MODE_NINE_SLICE = 2;
    public static final int MAX_BACKGROUND_LENGTH = 256;

    public ClusterStyle {
        background = background == null ? "" : background;
        backgroundMode = Math.max(0, Math.min(2, backgroundMode));
        sliceBorder = Math.max(0, Math.min(64, sliceBorder));
    }

    public ClusterStyle(int titleColor, int subtitleColor, int borderColor, int frameColor) {
        this(titleColor, subtitleColor, borderColor, frameColor, "", 0, 4, false, false);
    }

    public int titleOr() {
        return titleColor != 0 ? titleColor : DEFAULT_TITLE;
    }

    public int subtitleOr() {
        return subtitleColor != 0 ? subtitleColor : DEFAULT_SUBTITLE;
    }

    public boolean hasBackground() {
        return !background.isEmpty();
    }

    public boolean isDefault() {
        return equals(DEFAULT);
    }

    public ClusterStyle withTitle(int c) {
        return new ClusterStyle(c, subtitleColor, borderColor, frameColor, background, backgroundMode, sliceBorder, backgroundOnFrame, freeBox);
    }

    public ClusterStyle withSubtitle(int c) {
        return new ClusterStyle(titleColor, c, borderColor, frameColor, background, backgroundMode, sliceBorder, backgroundOnFrame, freeBox);
    }

    public ClusterStyle withBorder(int c) {
        return new ClusterStyle(titleColor, subtitleColor, c, frameColor, background, backgroundMode, sliceBorder, backgroundOnFrame, freeBox);
    }

    public ClusterStyle withFrame(int c) {
        return new ClusterStyle(titleColor, subtitleColor, borderColor, c, background, backgroundMode, sliceBorder, backgroundOnFrame, freeBox);
    }

    public ClusterStyle withBackground(String texture, int mode, int border, boolean onFrame) {
        return new ClusterStyle(titleColor, subtitleColor, borderColor, frameColor, texture, mode, border, onFrame, freeBox);
    }

    public ClusterStyle withFreeBox(boolean free) {
        return new ClusterStyle(titleColor, subtitleColor, borderColor, frameColor, background, backgroundMode, sliceBorder, backgroundOnFrame, free);
    }
}
