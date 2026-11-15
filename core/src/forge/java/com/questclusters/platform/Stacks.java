package com.questclusters.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Item stack data whose form differs by version (1.21: data components, 1.20.1: the stack's NBT). */
public final class Stacks {
    private Stacks() {
    }

    /** A copy of the stack's custom data (empty if none). */
    public static CompoundTag customData(ItemStack stack) {
        return stack.getTag() == null ? new CompoundTag() : stack.getTag().copy();
    }

    /** Sets the custom data; an empty tag removes it. */
    public static void setCustomData(ItemStack stack, CompoundTag tag) {
        stack.setTag(tag.isEmpty() ? null : tag);
    }
}
