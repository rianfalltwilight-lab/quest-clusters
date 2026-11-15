package com.questclusters.filters;

import com.questclusters.filters.filter.FilterIds;
import com.questclusters.filters.filter.FuelFilter;
import com.questclusters.filters.filter.ItemListFilter;
import com.questclusters.filters.filter.Lookups;
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
        //? if neoforge {
        reg.register(FilterIds.ITEMS, ItemListFilter::fromString, ItemListFilter::new);
        reg.register(FilterIds.ITEM_TAGS, TagListFilter.Items::fromString, TagListFilter.Items::new);
        reg.register(FilterIds.BLOCK_TAGS, TagListFilter.Blocks::fromString, TagListFilter.Blocks::new);
        reg.register(FilterIds.FUEL, FuelFilter::fromString, FuelFilter::new);
        //?} else {
        /*// FTB Filter System for 1.20.1 passes no registries: parse with the built-in ones
        reg.register(FilterIds.ITEMS, (p, a) -> ItemListFilter.fromString(p, a, Lookups.builtin()), ItemListFilter::new);
        reg.register(FilterIds.ITEM_TAGS, (p, a) -> TagListFilter.Items.fromString(p, a, Lookups.builtin()), TagListFilter.Items::new);
        reg.register(FilterIds.BLOCK_TAGS, (p, a) -> TagListFilter.Blocks.fromString(p, a, Lookups.builtin()), TagListFilter.Blocks::new);
        reg.register(FilterIds.FUEL, (p, a) -> FuelFilter.fromString(p, a, Lookups.builtin()), FuelFilter::new);
        *///?}

        wrapItemFilter(reg, false);
    }

    private static boolean wrapped;

    /**
     * FTB Filter System Addons 1.0.0 wrote several items as item(a,b,...), which FTB Filter System cannot read: read
     * those as an item list; a single item(a) stays FTB Filter System's own filter. FTB Filter System registers its
     * own filters in the same event, so if it has not yet, this runs again once loading is complete.
     */
    public static void wrapItemFilter(FTBFilterSystemRegistry reg, boolean last) {
        if (wrapped) return;
        ResourceLocation itemId = FTBFilterSystemAPI.rl("item");
        if (reg instanceof FilterRegistry fr) {
            fr.getDetails(itemId).ifPresentOrElse(details -> {
                SmartFilter.Factory original = details.factory();
                //? if neoforge {
                SmartFilter.Factory<SmartFilter> wrapped = (parent, arg, registries) -> arg.indexOf(',') >= 0
                        ? ItemListFilter.fromString(parent, arg, registries)
                        : (SmartFilter) original.create(parent, arg, registries);
                //?} else {
                /*SmartFilter.Factory<SmartFilter> wrapped = (parent, arg) -> arg.indexOf(',') >= 0
                        ? ItemListFilter.fromString(parent, arg, com.questclusters.filters.filter.Lookups.builtin())
                        : (SmartFilter) original.create(parent, arg);
                *///?}
                try {
                    reg.register(itemId, wrapped, (SmartFilter.DefaultFactory) details.defaultSupplier());
                } catch (IllegalStateException frozen) {
                    // already frozen (FTB Filter System registered its own filters after ours): swap the entry itself
                    try {
                        var field = FilterRegistry.class.getDeclaredField("filterMap");
                        field.setAccessible(true);
                        ((java.util.Map) field.get(fr)).put(itemId, new FilterRegistry.FilterDetails(itemId, wrapped, details.defaultSupplier()));
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        QuestClustersFilters.LOGGER.warn("Could not extend FTB Filter System's item filter; item(a,b) will not be readable", e);
                        return;
                    }
                }
                FilterRegistration.wrapped = true;
            }, () -> {
                if (last) QuestClustersFilters.LOGGER.warn("FTB Filter System's item filter not found; item(a,b) will not be readable");
            });
        } else {
            QuestClustersFilters.LOGGER.warn("Unexpected FTB Filter System registry {}; item(a,b) will not be readable", reg.getClass());
        }
    }
}
