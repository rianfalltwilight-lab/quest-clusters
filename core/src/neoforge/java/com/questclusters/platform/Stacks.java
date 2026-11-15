package com.questclusters.platform;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Item stack data whose form differs by version (1.21: data components, 1.20.1: the stack's NBT). */
public final class Stacks {
    private Stacks() {
    }

    /** A copy of the stack's custom data (empty if none). */
    public static CompoundTag customData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    /** Sets the custom data; an empty tag removes it. */
    public static void setCustomData(ItemStack stack, CompoundTag tag) {
        if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
