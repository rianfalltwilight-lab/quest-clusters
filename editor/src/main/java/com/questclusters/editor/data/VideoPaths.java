package com.questclusters.editor.data;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Video paths and the description video markup, in Quest Enhance's formats. A path is relative: either inside the
 * videos folder ({@code config/quest_clusters_editor/videos/}, falling back to Quest Enhance's
 * {@code config/quest_enhance/assets/quest_enhance/videos/ftb/}), or {@code assets/<rel>} for a video shipped in a
 * resource pack at {@code <namespace>:videos/<rel>}.
 */
public final class VideoPaths {
    public static final List<String> EXTENSIONS = List.of(".mp4", ".webm", ".mkv", ".mov", ".avi", ".m4v", ".ogv");
    public static final String KEY = "quest_enhance_video", LABEL = "quest_enhance_video_label", LEGACY_TEXT = "quest_enhance_video_text";
    /** Click value prefix of a description video link. */
    public static final String CLICK = "quest_enhance_video/";

    private VideoPaths() {
    }

    /** Normalised relative path ("/"-separated), or null if invalid (blank, ":" , absolute, escaping with ".."). */
    public static String normalise(String raw) {
        if (raw == null) return null;
        String s = raw.trim().replace('\\', '/');
        while (s.startsWith("/")) s = s.substring(1);
        if (s.isBlank() || s.contains(":")) return null;
        Path p;
        try {
            p = Path.of(s).normalize();
        } catch (Exception e) {
            return null;
        }
        String n = p.toString().replace('\\', '/');
        if (p.isAbsolute() || n.equals("..") || n.startsWith("../") || n.isBlank()) return null;
        return n;
    }

    public static boolean isVideoFile(String name) {
        String l = name.toLowerCase(Locale.ROOT);
        return EXTENSIONS.stream().anyMatch(l::endsWith);
    }

    public static boolean isBundled(String normalised) {
        return normalised.startsWith("assets/");
    }

    public static String fileName(String path) {
        int i = path.lastIndexOf('/');
        return i < 0 ? path : path.substring(i + 1);
    }

    // ------------------------------------------------------------------ description markup

    public static String descriptionLine(String path, String label) {
        String n = normalise(path);
        if (n == null) throw new IllegalArgumentException("Invalid video path: " + path);
        StringBuilder b = new StringBuilder("{").append(KEY).append(':').append(encode(n));
        if (label != null && !label.isBlank()) b.append(' ').append(LABEL).append(':').append(escape(label));
        return b.append('}').toString();
    }

    public static String encode(String path) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(path.getBytes(StandardCharsets.UTF_8));
    }

    /** Decoded, normalised path; null if invalid. */
    public static String decode(String b64) {
        try {
            return normalise(new String(Base64.getUrlDecoder().decode(b64.trim()), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    /** Label of a parsed description video line (FTB has already turned %20 into spaces). */
    public static String label(Map<String, String> props, String path) {
        String l = props.get(LABEL);
        if (l != null) return unescape(l);
        String legacy = props.get(LEGACY_TEXT);
        if (legacy != null) {
            try {
                return new String(Base64.getUrlDecoder().decode(legacy), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    static String escape(String s) {
        return s.replace("%", "%25").replace("{", "%7B").replace("}", "%7D").replace(" ", "%20");
    }

    static String unescape(String s) {
        return s.replace("%20", " ").replace("%7B", "{").replace("%7D", "}").replace("%25", "%");
    }
}
