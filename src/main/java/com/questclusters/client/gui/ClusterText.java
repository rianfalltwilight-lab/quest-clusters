package com.questclusters.client.gui;

import com.questclusters.data.QuestCluster;
import net.minecraft.network.chat.Component;

/**
 * Display text of a cluster. Like FTB Quests, a value written as {@code {some.translation.key}} is translated,
 * so modpacks can localise cluster titles through resource-pack lang files.
 */
public final class ClusterText {
    private ClusterText() {
    }

    public static Component title(QuestCluster c) {
        return text(c.title());
    }

    public static Component subtitle(QuestCluster c) {
        return text(c.subtitle());
    }

    public static Component text(String raw) {
        if (raw.length() > 2 && raw.startsWith("{") && raw.endsWith("}") && raw.indexOf(' ') < 0) {
            return Component.translatable(raw.substring(1, raw.length() - 1));
        }
        return Component.literal(raw);
    }
}
