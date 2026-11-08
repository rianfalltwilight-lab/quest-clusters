package com.questclusters.xmod.recipe;

import dev.ftb.mods.ftbquests.quest.Quest;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.function.Predicate;

/**
 * One quest as a recipe: what the tasks ask for on the left, what the rewards give on the right.
 * Shared by the JEI and REI plugins, which only lay it out.
 */
public record QuestRecipe(Quest quest, List<Slot> inputs, List<Slot> outputs) {
    public static final int GRID = 9;

    /**
     * One slot. {@code items} cycle in the slot; {@code fluid} (with {@code fluidAmount}) is set for fluid tasks.
     * {@code accepts} answers "is this stack used here?" exactly as the task tests it (tags, filters, components),
     * {@code overflow} counts entries folded into this slot because there were more than {@link #GRID}.
     */
    public record Slot(List<ItemStack> items, Fluid fluid, long fluidAmount, DataComponentPatch fluidComponents,
                       Predicate<ItemStack> accepts, int overflow) {
        public static Slot ofItems(List<ItemStack> items, Predicate<ItemStack> accepts) {
            return new Slot(List.copyOf(items), null, 0, DataComponentPatch.EMPTY, accepts, 0);
        }

        public boolean isFluid() {
            return fluid != null;
        }
    }

    public Component title() {
        return quest.getTitle();
    }

    public boolean usesItem(ItemStack stack) {
        for (Slot slot : inputs) {
            if (!slot.isFluid() && slot.accepts.test(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean usesFluid(Fluid fluid) {
        for (Slot slot : inputs) {
            if (slot.isFluid() && slot.fluid.isSame(fluid)) {
                return true;
            }
        }
        return false;
    }

    public boolean givesItem(ItemStack stack) {
        for (Slot slot : outputs) {
            for (ItemStack s : slot.items) {
                if (ItemStack.isSameItemSameComponents(s, stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Every item shown on one side, for indexing lookups by item. */
    static void collectItems(List<Slot> slots, java.util.Set<Item> into) {
        for (Slot slot : slots) {
            for (ItemStack s : slot.items) {
                into.add(s.getItem());
            }
        }
    }
}
