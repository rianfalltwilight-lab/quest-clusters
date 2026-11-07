package com.questclusters.client.gui;

import com.questclusters.data.QuestCluster;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Display text of a cluster, parsed exactly like FTB Quests parses quest titles: JSON text components
 * ({"text":"...","color":"gold"}), &amp; colour/format codes (&amp;6, &amp;l, ...) and {lang.key} translations.
 * Parsed results are cached because the text is drawn every frame.
 */
public final class ClusterText {
    private static final int CACHE_SIZE = 512;
    private static final Map<String, Component> CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Component> e) {
            return size() > CACHE_SIZE;
        }
    };

    private ClusterText() {
    }

    public static Component title(QuestCluster c) {
        return text(c.title());
    }

    public static Component subtitle(QuestCluster c) {
        return text(c.subtitle());
    }

    public static Component text(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        synchronized (CACHE) {
            Component cached = CACHE.get(raw);
            if (cached != null) {
                return cached;
            }
        }
        Component parsed = parse(raw);
        synchronized (CACHE) {
            CACHE.put(raw, parsed);
        }
        return parsed;
    }

    /** Plain text without formatting, e.g. for logs and history entries. */
    public static String plain(String raw) {
        return text(raw).getString();
    }

    private static Component parse(String raw) {
        if (FMLEnvironment.dist.isClient()) {
            try {
                return ClientParse.parse(raw);
            } catch (Throwable t) {
                // fall through to the simple rules below
            }
        }
        if (raw.length() > 2 && raw.startsWith("{") && raw.endsWith("}") && raw.indexOf(' ') < 0 && raw.indexOf('"') < 0) {
            return Component.translatable(raw.substring(1, raw.length() - 1));
        }
        return Component.literal(raw);
    }

    /** Separate class so dedicated servers never load FTB Quests' client text utilities. */
    private static final class ClientParse {
        static Component parse(String raw) {
            var file = dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE;
            var lookup = file != null ? file.holderLookup()
                    : net.minecraft.client.Minecraft.getInstance().level != null
                    ? net.minecraft.client.Minecraft.getInstance().level.registryAccess() : null;
            if (lookup == null) {
                throw new IllegalStateException("no registries yet");
            }
            return dev.ftb.mods.ftbquests.util.TextUtils.parseRawText(raw, lookup);
        }
    }

    /** Called when the language changes or the client leaves a world. */
    public static void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
}
