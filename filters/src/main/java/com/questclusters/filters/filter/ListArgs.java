package com.questclusters.filters.filter;

import java.util.ArrayList;
import java.util.List;

/** Text helpers for list arguments: splitting on top-level commas, percent-encoding variant entries. */
public final class ListArgs {
    private ListArgs() {
    }

    /** Splits on commas outside [], {} and quotes; trims; drops empty parts. */
    public static List<String> split(String s) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        char quote = 0;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == '\\' && i + 1 < s.length()) {
                    cur.append(c).append(s.charAt(++i));
                    continue;
                }
                if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '[' || c == '{') {
                depth++;
            } else if (c == ']' || c == '}') {
                depth = Math.max(0, depth - 1);
            } else if (c == ',' && depth == 0) {
                add(out, cur);
                cur.setLength(0);
                continue;
            }
            cur.append(c);
        }
        add(out, cur);
        return out;
    }

    private static void add(List<String> out, StringBuilder b) {
        String t = b.toString().trim();
        if (!t.isEmpty()) out.add(t);
    }

    /**
     * FTB Filter System strips one level of backslash escapes at every nesting level and splits on parentheses, so
     * these characters are percent-encoded inside entries.
     */
    public static String encode(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '%' -> b.append("%25");
                case '(' -> b.append("%28");
                case ')' -> b.append("%29");
                case '\\' -> b.append("%5C");
                default -> b.append(c);
            }
        }
        return b.toString();
    }

    public static String decode(String s) {
        if (s.indexOf('%') < 0) return s;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
                String hex = s.substring(i + 1, i + 3).toUpperCase(java.util.Locale.ROOT);
                switch (hex) {
                    case "25" -> b.append('%');
                    case "28" -> b.append('(');
                    case "29" -> b.append(')');
                    case "5C" -> b.append('\\');
                    default -> {
                        b.append(c);
                        continue;
                    }
                }
                i += 2;
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    /** A tag id as written in filters: without '#', with the namespace defaulted to minecraft. Null if invalid. */
    public static String normaliseTag(String s) {
        String t = s.trim();
        if (t.startsWith("#")) t = t.substring(1);
        if (t.isEmpty()) return null;
        if (t.indexOf(':') < 0) t = "minecraft:" + t;
        return net.minecraft.resources.ResourceLocation.tryParse(t) == null ? null : t;
    }
}
