package com.questclusters.filters.client;

import net.minecraft.world.item.ItemStack;

/** Public access to picker entries for screens in other packages. */
public final class ItemSourcesAccess {
    private ItemSourcesAccess() {
    }

    public static ItemSources.Choice choice(ItemStack stack, boolean variant) {
        return ItemSources.choice(stack, variant);
    }
}
