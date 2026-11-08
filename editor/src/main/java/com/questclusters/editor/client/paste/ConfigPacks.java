package com.questclusters.editor.client.paste;

import com.questclusters.editor.QuestClustersEditor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Always-on client resource packs from the config folder: ours (pasted images) and Quest Enhance's old one, if it
 * exists (its pasted images and GIFs, referenced by books made with it).
 */
public final class ConfigPacks {
    private ConfigPacks() {
    }

    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        add(event, PastedImages.packRoot(), "quest_clusters_editor/config", "Quest Clusters Editor: pasted images", true);
        add(event, PastedImages.legacyPackRoot(), "quest_clusters_editor/quest_enhance_config", "Quest Enhance: pasted images", false);
    }

    private static void add(AddPackFindersEvent event, Path root, String id, String title, boolean create) {
        try {
            if (!Files.isDirectory(root)) {
                if (!create) return;
                Files.createDirectories(root.resolve("assets/" + QuestClustersEditor.MOD_ID));
            }
            Path meta = root.resolve("pack.mcmeta");
            if (!Files.exists(meta)) {
                Files.writeString(meta, "{\"pack\":{\"description\":\"" + title + "\",\"pack_format\":34}}");
            }
        } catch (IOException e) {
            QuestClustersEditor.LOGGER.warn("Could not prepare resource pack folder {}: {}", root, e.toString());
            return;
        }
        PackLocationInfo info = new PackLocationInfo(id, Component.literal(title), PackSource.BUILT_IN, Optional.empty());
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(info, new PathPackResources.PathResourcesSupplier(root), PackType.CLIENT_RESOURCES,
                    new PackSelectionConfig(true, Pack.Position.TOP, false));
            if (pack != null) consumer.accept(pack);
        });
    }
}
