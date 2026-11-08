package com.questclusters.filters.client;

import com.questclusters.filters.client.screen.FuelScreen;
import com.questclusters.filters.client.screen.ItemListScreen;
import com.questclusters.filters.client.screen.TagListScreen;
import com.questclusters.filters.filter.FilterIds;
import com.questclusters.filters.filter.FuelFilter;
import com.questclusters.filters.filter.ItemListFilter;
import com.questclusters.filters.filter.TagListFilter;
import dev.ftb.mods.ftbfiltersystem.api.event.client.ClientFilterRegistrationEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/** Client setup: config screens for the filters. */
public final class FiltersClient {
    private FiltersClient() {
    }

    public static void init(IEventBus modBus) {
        ClientFilterRegistrationEvent.REGISTER.register(api -> {
            api.<ItemListFilter>registerFilterScreenFactory(FilterIds.ITEMS, ItemListScreen::new);
            api.<TagListFilter.Items>registerFilterScreenFactory(FilterIds.ITEM_TAGS, (f, p) -> new TagListScreen<>(f, p, false));
            api.<TagListFilter.Blocks>registerFilterScreenFactory(FilterIds.BLOCK_TAGS, (f, p) -> new TagListScreen<>(f, p, true));
            api.<FuelFilter>registerFilterScreenFactory(FilterIds.FUEL, FuelScreen::new);
        });
        if (System.getProperty("quest_clusters.devtest") != null) {
            FiltersDevTest.register();
        }
        // resource packs can rename items: searches must see the new names
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener(
                (ResourceManagerReloadListener) rm -> ItemSources.clearCache()));
    }
}
