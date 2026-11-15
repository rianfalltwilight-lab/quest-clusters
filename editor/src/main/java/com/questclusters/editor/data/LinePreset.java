package com.questclusters.editor.data;

/**
 * A named look for decorative lines: an image repeated along the line, drawn with its own colours.
 *
 * @param id      stable id referenced by chapters and nodes (short random hex)
 * @param name    shown in menus
 * @param texture image reference as FTB Library's image picker returns it (sprite id or texture path), or a
 *                .gif texture; images with a .mcmeta frame animation and GIFs play
 * @param scroll  how fast the image flows along the line, in image repeats per second (negative: towards the
 *                start); 0 = still
 * @param color   tint multiplied with the image (ARGB); white keeps the image's own colours
 * @param width   line thickness relative to the built-in line, 0.25 to 4
 */
public record LinePreset(String id, String name, String texture, float scroll, int color, float width) {
    public static final int MAX = 64;
    public static final int MAX_NAME = 64;
    public static final float MAX_SCROLL = 20F;
    public static final float MIN_WIDTH = 0.25F, MAX_WIDTH = 4F;
    public static final int WHITE = 0xFFFFFFFF;

    public LinePreset {
        id = id == null ? "" : id;
        name = name == null ? "" : name;
        texture = texture == null ? "" : texture;
    }

    public LinePreset(String id, String name, String texture) {
        this(id, name, texture, 0F);
    }

    public LinePreset(String id, String name, String texture, float scroll) {
        this(id, name, texture, scroll, WHITE, 1F);
    }

    public LinePreset withName(String newName) {
        return new LinePreset(id, newName, texture, scroll, color, width);
    }

    public LinePreset withTexture(String newTexture) {
        return new LinePreset(id, name, newTexture, scroll, color, width);
    }

    public LinePreset withScroll(float newScroll) {
        return new LinePreset(id, name, texture, newScroll, color, width);
    }

    public LinePreset withLook(int newColor, float newWidth) {
        return new LinePreset(id, name, texture, scroll, newColor, newWidth);
    }

    public boolean isValid() {
        return !id.isEmpty() && id.length() <= 32 && id.matches("[0-9a-z_]+") && name.length() <= MAX_NAME
                && texture.length() <= 256 && Float.isFinite(scroll) && Math.abs(scroll) <= MAX_SCROLL
                && Float.isFinite(width) && width >= MIN_WIDTH && width <= MAX_WIDTH;
    }

    public static String newId() {
        return Long.toHexString(java.util.concurrent.ThreadLocalRandom.current().nextLong() & 0xFFFFFFFFFFFFL);
    }
}
