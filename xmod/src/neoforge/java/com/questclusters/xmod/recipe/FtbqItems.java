package com.questclusters.xmod.recipe;

import dev.ftb.mods.ftbquests.registry.ModDataComponents;
import dev.ftb.mods.ftbquests.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** FTB Quests' items, and stack data, whose form differs between 1.21.1 and 1.20.1. */
public final class FtbqItems {
    private FtbqItems() {
    }

    public static Item book() {
        return ModItems.BOOK.get();
    }

    public static Item lootCrate() {
        return ModItems.LOOTCRATE.get();
    }

    /** The loot crate id of a crate stack, or null. */
    @Nullable
    public static String crateId(ItemStack stack) {
        return stack.get(ModDataComponents.LOOT_CRATE.get());
    }

    public static ItemStack named(ItemStack stack, Component name) {
        stack.set(DataComponents.CUSTOM_NAME, name);
        return stack;
    }
}
