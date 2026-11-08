package com.questclusters.editor.data;

/**
 * A named look for decorative lines: an image repeated along the line, drawn with its own colours.
 *
 * @param id      stable id referenced by chapters and nodes (short random hex)
 * @param name    shown in menus
 * @param texture image reference as FTB Library's image picker returns it (sprite id or texture path)
 */
public record LinePreset(String id, String name, String texture) {
    public static final int MAX = 64;
    public static final int MAX_NAME = 64;

    public LinePreset {
        id = id == null ? "" : id;
        name = name == null ? "" : name;
        texture = texture == null ? "" : texture;
    }

    public boolean isValid() {
        return !id.isEmpty() && id.length() <= 32 && id.matches("[0-9a-z_]+") && name.length() <= MAX_NAME
                && texture.length() <= 256;
    }

    public static String newId() {
        return Long.toHexString(java.util.concurrent.ThreadLocalRandom.current().nextLong() & 0xFFFFFFFFFFFFL);
    }
}
