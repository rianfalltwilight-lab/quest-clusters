package com.questclusters.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Computes a team's score and settlement content from its quest completions. Nothing is stored: resetting
 * progress, editing scores or deleting quests changes the result automatically, and old saves just work.
 */
public final class ScoreCalc {
    private ScoreCalc() {
    }

    /** Read-only view of one team's progress. */
    public interface Progress {
        boolean questExists(long questId);

        boolean isCompleted(long questId);

        /** Completion time in ms, or empty if unknown. */
        OptionalLong completedAt(long questId);

        long chapterOf(long questId);
    }

    public record Node(long questId, String text, int score, long time) {
    }

    /**
     * @param perChapter chapter id → {score earned, max score}, in first-seen order
     */
    public record Result(int total, int max, Map<Long, int[]> perChapter, List<Node> nodes, int completedScored) {
        public double percent() {
            return max <= 0 ? 0D : Math.max(0D, 100D * total / max);
        }
    }

    public static Result compute(Map<Long, ScoreEntry> entries, Progress p) {
        int total = 0, max = 0, completedScored = 0;
        Map<Long, int[]> perChapter = new LinkedHashMap<>();
        List<Node> nodes = new ArrayList<>();
        for (Map.Entry<Long, ScoreEntry> e : entries.entrySet()) {
            long id = e.getKey();
            ScoreEntry s = e.getValue();
            if (!p.questExists(id)) {
                continue;
            }
            boolean done = p.isCompleted(id);
            int[] ch = perChapter.computeIfAbsent(p.chapterOf(id), k -> new int[2]);
            if (s.score() > 0) {
                max += s.score();
                ch[1] += s.score();
            }
            if (!done) {
                continue;
            }
            if (s.score() != 0) {
                total += s.score();
                ch[0] += s.score();
                completedScored++;
            }
            if (s.isNode()) {
                nodes.add(new Node(id, s.node(), s.score(), p.completedAt(id).orElse(Long.MAX_VALUE)));
            }
        }
        nodes.sort(Comparator.comparingLong(Node::time).thenComparingLong(Node::questId));
        perChapter.values().removeIf(v -> v[0] == 0 && v[1] == 0);
        return new Result(total, max, perChapter, nodes, completedScored);
    }

    /** One leaderboard row. */
    public record Row(String team, int quests, int score, boolean own) {
    }

    /** Most quests first, then score, then name. */
    public static List<Row> sort(List<Row> rows) {
        List<Row> out = new ArrayList<>(rows);
        out.sort(Comparator.comparingInt(Row::quests).reversed()
                .thenComparing(Comparator.comparingInt(Row::score).reversed())
                .thenComparing(Row::team, String.CASE_INSENSITIVE_ORDER));
        return out;
    }
}
