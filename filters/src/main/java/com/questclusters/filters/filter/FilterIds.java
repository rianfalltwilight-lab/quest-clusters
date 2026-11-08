package com.questclusters.filters.filter;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Filter ids. The filters are written under Smart Filter Extras' ids, so filter strings saved in item stacks,
 * machines, pipes and quest tasks stay interchangeable with it; older ids are read as aliases.
 */
public final class FilterIds {
    public static final String NS = "smart_filter_extras";
    public static final ResourceLocation ITEMS = rl("items"), ITEM_TAGS = rl("item_tag"), BLOCK_TAGS = rl("block_tag"), FUEL = rl("fuel");

    /** Older ids (Smart Filter Extras 2.x and FTB Filter System Addons 1.0.0) → current id. */
    public static final Map<String, ResourceLocation> ALIASES = Map.of(
            "smartfilterextras:items", ITEMS,
            "smartfilterextras:item_tag", ITEM_TAGS,
            "smartfilterextras:block_tag", BLOCK_TAGS,
            "smartfilterextras:fuel", FUEL,
            "ftbfiltersystemaddons:block_tag", BLOCK_TAGS);

    private FilterIds() {
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(NS, path);
    }
}
