package com.questclusters.visuals.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Parsed entity icon string, same syntax as Certain Questing Additions:
 * <pre>entity:&lt;entity id&gt;[key=value, ...]{snbt}</pre>
 * Keys: rotate/rotation/auto_rotate, look/look_at_cursor; for minecraft:player also skin/nick/name/username/player,
 * display_name/displayname/player_name/nickname/nick_name/label/name_tag/nametag, show_name/show_nickname/show_nick,
 * uuid/player_uuid/profile_uuid; nbt=&lt;snbt&gt;. Any other key=value is merged into the entity NBT.
 * Pure Java (parsing only), so it can be unit-tested.
 */
public record EntityIconSpec(String entityId, boolean rotate, boolean lookAtCursor, String skin, String displayName,
                             boolean showName, UUID uuid, String nbt, Map<String, String> extraNbt) {
    public static final String PREFIX = "entity:";
    public static final String PLAYER = "minecraft:player";

    public boolean isPlayer() {
        return PLAYER.equals(entityId);
    }

    /** @return null if the string is not a valid entity icon */
    public static EntityIconSpec parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith(PREFIX)) s = s.substring(PREFIX.length());
        int firstExtra = firstOf(s, '[', '{');
        String id = (firstExtra < 0 ? s : s.substring(0, firstExtra)).trim();
        if (id.isEmpty() || !id.matches("([a-z0-9_.-]+:)?[a-z0-9_./-]+")) return null;
        if (!id.contains(":")) id = "minecraft:" + id;

        List<String> options = List.of();
        String nbt = "";
        int i = firstExtra < 0 ? s.length() : firstExtra;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '[' || c == '{') {
                int end = closing(s, i);
                if (end < 0) return null;
                if (c == '[') options = splitTopLevel(s.substring(i + 1, end));
                else nbt = s.substring(i, end + 1);
                i = end + 1;
            } else {
                return null;
            }
        }

        boolean player = PLAYER.equals(id);
        boolean rotate = false, look = false, showName = true;
        String skin = "", display = "", optNbt = "";
        UUID uuid = null;
        Map<String, String> extra = new LinkedHashMap<>();
        for (String entry : options) {
            int eq = entry.indexOf('=');
            if (eq <= 0) continue;
            String key = entry.substring(0, eq).trim();
            String value = entry.substring(eq + 1).trim();
            String k = key.toLowerCase(Locale.ROOT);
            switch (k) {
                case "rotate", "rotation", "auto_rotate" -> {
                    Boolean b = bool(value);
                    if (b == null) return null;
                    rotate = b;
                }
                case "look", "look_at_cursor", "lookatcursor" -> {
                    Boolean b = bool(value);
                    if (b == null) return null;
                    look = b;
                }
                case "nbt" -> optNbt = value;
                default -> {
                    if (player && List.of("skin", "nick", "name", "username", "player").contains(k)) skin = unquote(value);
                    else if (player && List.of("display_name", "displayname", "player_name", "nickname", "nick_name", "label", "name_tag", "nametag").contains(k))
                        display = unquote(value);
                    else if (player && List.of("show_name", "show_nickname", "show_nick").contains(k)) {
                        Boolean b = bool(value);
                        if (b == null) return null;
                        showName = b;
                    } else if (player && List.of("uuid", "player_uuid", "profile_uuid").contains(k)) uuid = uuid(value);
                    else extra.put(key, value);
                }
            }
        }
        return new EntityIconSpec(id, rotate, look, skin, display, showName, uuid, nbt.isBlank() ? optNbt : nbt, extra);
    }

    private static int firstOf(String s, char a, char b) {
        int x = s.indexOf(a), y = s.indexOf(b);
        return x < 0 ? y : y < 0 ? x : Math.min(x, y);
    }

    /** Index of the bracket closing the one at {@code open}, honouring nesting and quotes; -1 if unbalanced. */
    static int closing(String s, int open) {
        int depth = 0;
        char quote = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == '\\') i++;
                else if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '[' || c == '{' || c == '(') {
                depth++;
            } else if (c == ']' || c == '}' || c == ')') {
                if (--depth == 0) return i;
            }
        }
        return -1;
    }

    static List<String> splitTopLevel(String s) {
        List<String> out = new ArrayList<>();
        int depth = 0, start = 0;
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == '\\') i++;
                else if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') quote = c;
            else if (c == '[' || c == '{' || c == '(') depth++;
            else if (c == ']' || c == '}' || c == ')') depth--;
            else if ((c == ',' || c == ';') && depth == 0) {
                if (!s.substring(start, i).isBlank()) out.add(s.substring(start, i).trim());
                start = i + 1;
            }
        }
        if (!s.substring(start).isBlank()) out.add(s.substring(start).trim());
        return out;
    }

    static Boolean bool(String v) {
        return switch (unquote(v).toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> null;
        };
    }

    static String unquote(String v) {
        String t = v.trim();
        if (t.length() >= 4 && (t.startsWith("\\\"") && t.endsWith("\\\"") || t.startsWith("\\'") && t.endsWith("\\'")))
            return t.substring(2, t.length() - 2);
        if (t.length() >= 2 && (t.startsWith("\"") && t.endsWith("\"") || t.startsWith("'") && t.endsWith("'")))
            return t.substring(1, t.length() - 1);
        return t;
    }

    static UUID uuid(String v) {
        String t = unquote(v);
        if (t.length() == 32 && t.matches("[0-9a-fA-F]+")) {
            t = t.substring(0, 8) + "-" + t.substring(8, 12) + "-" + t.substring(12, 16) + "-" + t.substring(16, 20) + "-" + t.substring(20);
        }
        try {
            return UUID.fromString(t);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
