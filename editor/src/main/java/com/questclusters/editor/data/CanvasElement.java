package com.questclusters.editor.data;

import java.util.UUID;

/**
 * Special canvas elements stored in a chapter image's click action data (action type "none"), in the format
 * Quest Enhance books already use, so such images stay plain FTB Quests images if the editor is removed.
 */
public sealed interface CanvasElement {
    String PREFIX = "quest_enhance:";
    String TEXT_V1 = PREFIX + "text:";
    String TEXT_V2 = PREFIX + "text_v2:";
    String TEXT_V3 = PREFIX + "text_v3:";
    String GIF = PREFIX + "gif_v1:";
    String VIDEO = PREFIX + "video_v1:";
    String ANCHOR = PREFIX + "decorative_anchor:";
    String DEFAULT_FONT = "minecraft:default";

    record Text(String text, String font, double scale) implements CanvasElement {
        public String encode() {
            double s = Double.isFinite(scale) ? Math.max(0.05, Math.min(10, scale)) : 1.0;
            return TEXT_V3 + s + ":" + font.length() + ":" + font + text;
        }
    }

    record Gif(String resource) implements CanvasElement {
        public String encode() {
            return GIF + resource;
        }
    }

    record Video(String path) implements CanvasElement {
        public String encode() {
            return VIDEO + path;
        }
    }

    /** @param uuid null if the stored id does not parse (still an anchor, but not usable as a line node) */
    record Anchor(String uuid) implements CanvasElement {
        public String nodeKey() {
            return uuid == null ? null : NodeKeys.anchor(uuid);
        }

        public String encode() {
            return ANCHOR + (uuid == null ? "" : uuid);
        }
    }

    /** Parses click action data; null if it is not a (valid) special element. */
    static CanvasElement parse(String data) {
        if (data == null || !data.startsWith(PREFIX)) {
            return null;
        }
        if (data.startsWith(TEXT_V3)) {
            String s = data.substring(TEXT_V3.length());
            int i = s.indexOf(':');
            if (i <= 0) return null;
            int j = s.indexOf(':', i + 1);
            if (j <= i + 1) return null;
            try {
                double scale = Double.parseDouble(s.substring(0, i));
                int len = Integer.parseInt(s.substring(i + 1, j));
                if (!Double.isFinite(scale) || len <= 0 || j + 1 + len > s.length()) return null;
                String font = s.substring(j + 1, j + 1 + len);
                if (!validLocation(font)) return null;
                return new Text(s.substring(j + 1 + len), font, Math.max(0.05, Math.min(10, scale)));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (data.startsWith(TEXT_V2)) {
            String s = data.substring(TEXT_V2.length());
            int i = s.indexOf(':');
            if (i <= 0) return null;
            try {
                int len = Integer.parseInt(s.substring(0, i));
                if (len <= 0 || i + 1 + len > s.length()) return null;
                String font = s.substring(i + 1, i + 1 + len);
                if (!validLocation(font)) return null;
                return new Text(s.substring(i + 1 + len), font, 1.0);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (data.startsWith(TEXT_V1)) {
            return new Text(data.substring(TEXT_V1.length()), DEFAULT_FONT, 1.0);
        }
        if (data.startsWith(GIF)) {
            String rl = data.substring(GIF.length());
            return validLocation(rl) ? new Gif(rl) : null;
        }
        if (data.startsWith(VIDEO)) {
            String p = data.substring(VIDEO.length()).trim();
            return p.isEmpty() ? null : new Video(p);
        }
        if (data.startsWith(ANCHOR)) {
            String u = data.substring(ANCHOR.length());
            try {
                return new Anchor(UUID.fromString(u).toString());
            } catch (IllegalArgumentException e) {
                return new Anchor(null);
            }
        }
        return null;
    }

    /** Same character rules as Minecraft resource locations ("ns:path" or "path"). */
    static boolean validLocation(String s) {
        if (s == null || s.isEmpty()) return false;
        int c = s.indexOf(':');
        String ns = c < 0 ? "minecraft" : s.substring(0, c);
        String path = c < 0 ? s : s.substring(c + 1);
        if (ns.isEmpty() || path.isEmpty()) return false;
        for (char ch : ns.toCharArray()) {
            if (!(ch == '_' || ch == '-' || ch == '.' || ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9')) return false;
        }
        for (char ch : path.toCharArray()) {
            if (!(ch == '_' || ch == '-' || ch == '.' || ch == '/' || ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9')) return false;
        }
        return true;
    }
}
