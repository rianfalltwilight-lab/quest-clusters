package com.questclusters.data;

/**
 * Reason a cluster edit was rejected. {@code key} is a translation key under {@code quest_clusters.error.}.
 */
public record ValidationError(String key, Object... args) {
    public String translationKey() {
        return "quest_clusters.error." + key;
    }
}
