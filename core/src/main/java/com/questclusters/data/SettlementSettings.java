package com.questclusters.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Author-defined content of the settlement page. All text accepts FTB text formats (&amp; codes, JSON, {lang.key}).
 *
 * @param title        heading at the top
 * @param intro        lines before the node list
 * @param outro        lines after the totals
 * @param ranks        rank names by minimum percentage of the maximum score, e.g. 90 → "&amp;6S"
 * @param showMax      show "score / max" and percentage (off: only the score, max stays secret)
 * @param leaderboard  enable the leaderboard button and command
 * @param theme        look of the page, one of {@link #THEMES}
 * @param background   texture drawn behind everything ("" = the theme's own backdrop)
 * @param backgroundMode how the background fills the screen ({@link ClusterStyle#MODE_STRETCH} and friends)
 */
public record SettlementSettings(String title, List<String> intro, List<String> outro, List<Rank> ranks,
                                 boolean showMax, boolean leaderboard, String theme, String background, int backgroundMode) {
    /** @param image badge texture shown with the rank ("" = text only), {@code size} its height in px */
    public record Rank(int minPercent, String name, String image, int size) {
        public Rank(int minPercent, String name) {
            this(minPercent, name, "", 0);
        }

        public Rank {
            name = name == null ? "" : name;
            image = image == null ? "" : image;
            size = image.isEmpty() ? 0 : size <= 0 ? DEFAULT_BADGE : Math.min(256, size);
        }
    }

    /** Built-in looks: end credits, report card, neon starfield, terminal, award podium. */
    public static final List<String> THEMES = List.of("credits", "report", "neon", "terminal", "podium");
    public static final int DEFAULT_BADGE = 64;

    public static final SettlementSettings DEFAULT = new SettlementSettings("{quest_clusters.settlement.default_title}",
            List.of(), List.of(), List.of(new Rank(90, "&6S"), new Rank(75, "&eA"), new Rank(50, "&aB"), new Rank(0, "&7C")),
            true, true);
    public static final int MAX_LINES = 64;
    public static final int MAX_LINE_LENGTH = 512;

    public SettlementSettings(String title, List<String> intro, List<String> outro, List<Rank> ranks, boolean showMax, boolean leaderboard) {
        this(title, intro, outro, ranks, showMax, leaderboard, THEMES.get(0), "", ClusterStyle.MODE_STRETCH);
    }

    public SettlementSettings {
        title = title == null ? "" : title;
        theme = theme == null || !THEMES.contains(theme) ? THEMES.get(0) : theme;
        background = background == null ? "" : background;
        backgroundMode = Math.max(0, Math.min(2, backgroundMode));
        intro = intro == null ? List.of() : List.copyOf(intro);
        outro = outro == null ? List.of() : List.copyOf(outro);
        List<Rank> sorted = new ArrayList<>(ranks == null ? List.of() : ranks);
        sorted.sort(Comparator.comparingInt(Rank::minPercent).reversed());
        ranks = List.copyOf(sorted);
    }

    /** Rank for a percentage (0..100), or "" if no rank applies. */
    public String rankFor(double percent) {
        Rank r = rankOf(percent);
        return r == null ? "" : r.name();
    }

    /** The rank reached at a percentage (0..100), or null. */
    public Rank rankOf(double percent) {
        for (Rank r : ranks) {
            if (percent + 1e-9 >= r.minPercent()) {
                return r;
            }
        }
        return null;
    }

    public SettlementSettings withLook(String theme, String background, int backgroundMode) {
        return new SettlementSettings(title, intro, outro, ranks, showMax, leaderboard, theme, background, backgroundMode);
    }

    /** "90=&amp;6S" lines (with "|image=…|size=…" for a badge), the format used in the editor. */
    public List<String> rankLines() {
        return ranks.stream().map(r -> r.minPercent() + "=" + r.name()
                + (r.image().isEmpty() ? "" : "|image=" + r.image() + (r.size() == DEFAULT_BADGE ? "" : "|size=" + r.size()))).toList();
    }

    public static List<Rank> parseRanks(List<String> lines) {
        List<Rank> out = new ArrayList<>();
        for (String l : lines) {
            int eq = l.indexOf('=');
            if (eq <= 0) continue;
            try {
                int p = Integer.parseInt(l.substring(0, eq).trim());
                String[] parts = l.substring(eq + 1).split("\\|");
                String image = "";
                int size = 0;
                for (int i = 1; i < parts.length; i++) {
                    String o = parts[i].trim();
                    if (o.startsWith("image=")) image = o.substring(6).trim();
                    else if (o.startsWith("size=")) size = Integer.parseInt(o.substring(5).trim());
                }
                out.add(new Rank(Math.max(0, Math.min(100, p)), parts.length == 0 ? "" : parts[0].trim(), image, size));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    /**
     * An image line in the intro, outro or a node text, written like FTB Quests' description images:
     * {@code {image:ns:path.png width:64 height:32 align:left}}. Width and height default to the texture's own size
     * (height follows width when only width is given); align is left, center (default) or right.
     */
    public record ImageLine(String texture, int width, int height, int align) {
        public static final int LEFT = 0, CENTER = 1, RIGHT = 2;

        /** The image a whole line asks for, or null for ordinary text. */
        public static ImageLine parse(String line) {
            if (line == null) return null;
            String t = line.trim();
            if (!t.startsWith("{image:") || !t.endsWith("}")) return null;
            String[] parts = t.substring(7, t.length() - 1).trim().split("\\s+");
            if (parts.length == 0 || parts[0].isEmpty()) return null;
            int w = 0, h = 0, align = CENTER;
            for (int i = 1; i < parts.length; i++) {
                int c = parts[i].indexOf(':');
                if (c <= 0) continue;
                String k = parts[i].substring(0, c), v = parts[i].substring(c + 1);
                try {
                    switch (k) {
                        case "width" -> w = Math.max(0, Math.min(1024, Integer.parseInt(v)));
                        case "height" -> h = Math.max(0, Math.min(1024, Integer.parseInt(v)));
                        case "align" -> align = v.equals("left") || v.equals("0") ? LEFT : v.equals("right") || v.equals("2") ? RIGHT : CENTER;
                        default -> {
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return new ImageLine(parts[0], w, h, align);
        }
    }
}
