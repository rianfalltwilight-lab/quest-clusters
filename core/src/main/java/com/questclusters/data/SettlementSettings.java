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
 */
public record SettlementSettings(String title, List<String> intro, List<String> outro, List<Rank> ranks,
                                 boolean showMax, boolean leaderboard) {
    public record Rank(int minPercent, String name) {
    }

    public static final SettlementSettings DEFAULT = new SettlementSettings("{quest_clusters.settlement.default_title}",
            List.of(), List.of(), List.of(new Rank(90, "&6S"), new Rank(75, "&eA"), new Rank(50, "&aB"), new Rank(0, "&7C")),
            true, true);
    public static final int MAX_LINES = 64;
    public static final int MAX_LINE_LENGTH = 512;

    public SettlementSettings {
        title = title == null ? "" : title;
        intro = intro == null ? List.of() : List.copyOf(intro);
        outro = outro == null ? List.of() : List.copyOf(outro);
        List<Rank> sorted = new ArrayList<>(ranks == null ? List.of() : ranks);
        sorted.sort(Comparator.comparingInt(Rank::minPercent).reversed());
        ranks = List.copyOf(sorted);
    }

    /** Rank for a percentage (0..100), or "" if no rank applies. */
    public String rankFor(double percent) {
        for (Rank r : ranks) {
            if (percent + 1e-9 >= r.minPercent()) {
                return r.name();
            }
        }
        return "";
    }

    /** "90=&amp;6S" lines, the format used in the editor. */
    public List<String> rankLines() {
        return ranks.stream().map(r -> r.minPercent() + "=" + r.name()).toList();
    }

    public static List<Rank> parseRanks(List<String> lines) {
        List<Rank> out = new ArrayList<>();
        for (String l : lines) {
            int eq = l.indexOf('=');
            if (eq <= 0) continue;
            try {
                int p = Integer.parseInt(l.substring(0, eq).trim());
                out.add(new Rank(Math.max(0, Math.min(100, p)), l.substring(eq + 1).trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }
}
