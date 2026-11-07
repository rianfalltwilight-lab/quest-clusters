package com.questclusters.data;

/**
 * How a merged line is shown when some of the real lines it stands for are hidden by another addon.
 * A merged line disappears only if every real line behind it is hidden; it can be revealed on hover if at least
 * one hidden line allows that.
 */
public final class LineRule {
    private LineRule() {
    }

    public enum Visibility {
        SHOW,
        HIDDEN,
        HIDDEN_REVEAL_ON_HOVER
    }

    /**
     * @param total            real lines behind the merged line
     * @param hidden           how many of them are hidden
     * @param revealOnHover    how many of the hidden ones are set to reveal on hover
     */
    public static Visibility decide(int total, int hidden, int revealOnHover) {
        if (hidden < total) {
            return Visibility.SHOW;
        }
        return revealOnHover > 0 ? Visibility.HIDDEN_REVEAL_ON_HOVER : Visibility.HIDDEN;
    }
}
