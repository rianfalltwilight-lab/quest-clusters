package com.questclusters.filters.filter;

import dev.ftb.mods.ftbfiltersystem.api.NumericComparison;
import dev.ftb.mods.ftbfiltersystem.api.filter.AbstractSmartFilter;
import dev.ftb.mods.ftbfiltersystem.api.filter.SmartFilter;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

/** {@code fuel(>=1600)}: compares the furnace burn time in ticks (NeoForge's furnace_fuels data map). */
public class FuelFilter extends AbstractSmartFilter {
    private final NumericComparison comparison;

    public FuelFilter(@Nullable SmartFilter.Compound parent) {
        this(parent, new NumericComparison(NumericComparison.ComparisonOp.GT, 0, false));
    }

    public FuelFilter(@Nullable SmartFilter.Compound parent, NumericComparison comparison) {
        super(parent);
        this.comparison = comparison;
    }

    public NumericComparison comparison() {
        return comparison;
    }

    public static int burnTime(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getBurnTime(RecipeType.SMELTING);
    }

    @Override
    public ResourceLocation getId() {
        return FilterIds.FUEL;
    }

    @Override
    public boolean test(ItemStack stack) {
        return comparison.test(burnTime(stack));
    }

    @Override
    public String getStringArg(HolderLookup.Provider registryAccess) {
        return comparison.toString();
    }

    public static FuelFilter fromString(SmartFilter.Compound parent, String arg, HolderLookup.Provider registries) {
        return new FuelFilter(parent, NumericComparison.fromString(arg.replace(" ", ""), false));
    }
}
