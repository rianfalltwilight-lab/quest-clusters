package com.questclusters.filters;

import com.questclusters.filters.filter.FilterIds;
import com.questclusters.filters.filter.FuelFilter;
import com.questclusters.filters.filter.ItemListFilter;
import com.questclusters.filters.filter.TagListFilter;
import dev.ftb.mods.ftbfiltersystem.api.FTBFilterSystemAPI;
import dev.ftb.mods.ftbfiltersystem.api.FTBFilterSystemRegistry;
import dev.ftb.mods.ftbfiltersystem.api.filter.SmartFilter;
import dev.ftb.mods.ftbfiltersystem.registry.FilterRegistry;
import net.minecraft.resources.ResourceLocation;

/** Registers the filters with FTB Filter System (after its own built-ins). */
public final class FilterRegistration {
    private FilterRegistration() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register(FTBFilterSystemRegistry reg) {
        reg.register(FilterIds.ITEMS, ItemListFilter::fromString, ItemListFilter::new);
        reg.register(FilterIds.ITEM_TAGS, TagListFilter.Items::fromString, TagListFilter.Items::new);
        reg.register(FilterIds.BLOCK_TAGS, TagListFilter.Blocks::fromString, TagListFilter.Blocks::new);
        reg.register(FilterIds.FUEL, FuelFilter::fromString, FuelFilter::new);

        // FTB Filter System Addons 1.0.0 wrote several items as item(a,b,...), which FTB Filter System cannot read:
        // read those as an item list; a single item(a) stays FTB Filter System's own filter.
        ResourceLocation itemId = FTBFilterSystemAPI.rl("item");
        if (reg instanceof FilterRegistry fr) {
            fr.getDetails(itemId).ifPresentOrElse(details -> {
                SmartFilter.Factory original = details.factory();
                SmartFilter.Factory<SmartFilter> wrapped = (parent, arg, registries) -> arg.indexOf(',') >= 0
                        ? ItemListFilter.fromString(parent, arg, registries)
                        : (SmartFilter) original.create(parent, arg, registries);
                reg.register(itemId, wrapped, (SmartFilter.DefaultFactory) details.defaultSupplier());
            }, () -> QuestClustersFilters.LOGGER.warn("FTB Filter System's item filter not found; item(a,b) will not be readable"));
        } else {
            QuestClustersFilters.LOGGER.warn("Unexpected FTB Filter System registry {}; item(a,b) will not be readable", reg.getClass());
        }
    }
}
