package com.questclusters.xmod.filtering;

import com.questclusters.xmod.QuestClustersXMod;
import dev.ftb.mods.ftbfiltersystem.api.FTBFilterSystemAPI;
import dev.ftb.mods.ftbfiltersystem.api.FilterException;
import dev.ftb.mods.ftbfiltersystem.api.filter.SmartFilter;
import dev.ftb.mods.ftbquests.api.ItemFilterAdapter;
import dev.ftb.mods.ftbquests.integration.item_filtering.ItemMatchingSystem;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Lets FTB Quests item tasks take an FTB Filter System smart filter as their item: any matching item counts.
 * (Item Filters, the other filter mod XMod supports on older versions, does not exist for 1.21.1.)
 */
public final class FfsFilterAdapter implements ItemFilterAdapter {
    private FfsFilterAdapter() {
    }

    public static void register() {
        ItemMatchingSystem.INSTANCE.registerFilterAdapter(new FfsFilterAdapter());
        QuestClustersXMod.LOGGER.info("FTB Quests can use FTB Filter System smart filters in item tasks");
    }

    @Override
    public String getName() {
        return "FTB Filter System";
    }

    @Override
    public boolean isFilterStack(ItemStack stack) {
        return FTBFilterSystemAPI.api().isFilterItem(stack);
    }

    @Override
    public boolean doesItemMatch(ItemStack filterStack, ItemStack toCheck, HolderLookup.Provider registryAccess) {
        return FTBFilterSystemAPI.api().doesFilterMatch(filterStack, toCheck, registryAccess);
    }

    /** Parses the filter once for repeated checks (inventory scans); an unreadable filter matches nothing. */
    @Override
    public Matcher getMatcher(ItemStack filterStack, HolderLookup.Provider registryAccess) {
        try {
            SmartFilter filter = FTBFilterSystemAPI.api().parseFilter(filterStack, registryAccess);
            return filter::test;
        } catch (FilterException e) {
            return NO_MATCH;
        }
    }

    @Override
    public ItemStack makeTagFilterStack(TagKey<Item> tag) {
        return FTBFilterSystemAPI.api().makeTagFilter(tag);
    }
}
