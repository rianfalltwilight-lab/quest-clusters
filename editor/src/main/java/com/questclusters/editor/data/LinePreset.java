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
 */
public record LinePreset(String id, String name, String texture, float scroll) {
    public static final int MAX = 64;
    public static final int MAX_NAME = 64;
    public static final float MAX_SCROLL = 20F;

    public LinePreset {
        id = id == null ? "" : id;
        name = name == null ? "" : name;
        texture = texture == null ? "" : texture;
    }

    public LinePreset(String id, String name, String texture) {
        this(id, name, texture, 0F);
    }

    public LinePreset withScroll(float newScroll) {
        return new LinePreset(id, name, texture, newScroll);
    }

    public boolean isValid() {
        return !id.isEmpty() && id.length() <= 32 && id.matches("[0-9a-z_]+") && name.length() <= MAX_NAME
                && texture.length() <= 256 && Float.isFinite(scroll) && Math.abs(scroll) <= MAX_SCROLL;
    }

    public static String newId() {
        return Long.toHexString(java.util.concurrent.ThreadLocalRandom.current().nextLong() & 0xFFFFFFFFFFFFL);
    }
}
