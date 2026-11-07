package com.questclusters.editor.data;

/** Per-quest editor data. Empty strings mean "not set". */
public record QuestExtras(String boxBackground, String viewBackground, boolean disableScrollPaging) {
    public static final QuestExtras EMPTY = new QuestExtras("", "", false);

    public boolean isEmpty() {
        return boxBackground.isEmpty() && viewBackground.isEmpty() && !disableScrollPaging;
    }

    public QuestExtras withBoxBackground(String v) {
        return new QuestExtras(v == null ? "" : v, viewBackground, disableScrollPaging);
    }

    public QuestExtras withViewBackground(String v) {
        return new QuestExtras(boxBackground, v == null ? "" : v, disableScrollPaging);
    }

    public QuestExtras withDisableScrollPaging(boolean v) {
        return new QuestExtras(boxBackground, viewBackground, v);
    }
}
