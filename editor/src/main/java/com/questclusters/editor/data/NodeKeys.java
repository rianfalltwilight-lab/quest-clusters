package com.questclusters.editor.data;

/**
 * Canvas node keys, in Quest Enhance's format so imported books keep working: "q:&lt;hex&gt;" quest, "l:&lt;hex&gt;"
 * quest link, "a:&lt;uuid&gt;" guide point. Hex is lower case without zero padding.
 */
public final class NodeKeys {
    private NodeKeys() {
    }

    public static String quest(long id) {
        return "q:" + Long.toUnsignedString(id, 16);
    }

    public static String link(long id) {
        return "l:" + Long.toUnsignedString(id, 16);
    }

    public static String anchor(String uuid) {
        return "a:" + uuid;
    }

    /** Object id of a q:/l: key, or null for anchors and malformed keys. */
    public static Long objectId(String key) {
        if (key == null || key.length() < 3 || key.charAt(1) != ':' || (key.charAt(0) != 'q' && key.charAt(0) != 'l')) {
            return null;
        }
        try {
            return Long.parseUnsignedLong(key.substring(2), 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isAnchor(String key) {
        return key != null && key.startsWith("a:");
    }

    public static String hex(long id) {
        return String.format("%016X", id);
    }

    /** Parses FTB Quests' id spelling ("#"-prefixed or plain hex). Null if malformed. */
    public static Long parseHex(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.isEmpty() || s.length() > 16) return null;
        try {
            return Long.parseUnsignedLong(s, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
