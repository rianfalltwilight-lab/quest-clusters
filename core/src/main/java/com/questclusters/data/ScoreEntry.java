package com.questclusters.data;

/**
 * Scoring attributes of one quest.
 *
 * @param score            points the team gets when the quest is completed (0 = none)
 * @param node             text printed on the settlement page when completed ("" = not a node); FTB text formats
 * @param opensSettlement  completing this quest opens the settlement page for the team's online members
 */
public record ScoreEntry(int score, String node, boolean opensSettlement) {
    public static final int MAX_SCORE = 1_000_000;
    public static final int MAX_NODE_LENGTH = 512;

    public ScoreEntry {
        score = Math.max(-MAX_SCORE, Math.min(MAX_SCORE, score));
        node = node == null ? "" : node;
    }

    public boolean isEmpty() {
        return score == 0 && node.isEmpty() && !opensSettlement;
    }

    public boolean isNode() {
        return !node.isEmpty();
    }
}
