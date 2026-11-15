package com.questclusters.xmod.recipe;

import dev.ftb.mods.ftbquests.item.FTBQuestsItems;
import dev.ftb.mods.ftbquests.item.LootCrateItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** FTB Quests' items, and stack data, whose form differs between 1.21.1 and 1.20.1. */
public final class FtbqItems {
    private FtbqItems() {
    }

    public static Item book() {
        return FTBQuestsItems.BOOK.get();
    }

    public static Item lootCrate() {
        return FTBQuestsItems.LOOTCRATE.get();
    }

    /** The loot crate id of a crate stack, or null. */
    @Nullable
    public static String crateId(ItemStack stack) {
        if (!stack.is(lootCrate())) return null;
        var crate = LootCrateItem.getCrate(stack);
        return crate != null ? crate.getStringID() : null;
    }

    public static ItemStack named(ItemStack stack, Component name) {
        return stack.setHoverName(name);
    }
}
