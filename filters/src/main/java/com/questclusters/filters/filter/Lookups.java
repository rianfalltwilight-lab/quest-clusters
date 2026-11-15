package com.questclusters.filters.filter;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;

/** Registries for filter text where FTB Filter System passes none (its 1.20.1 version): the built-in ones. */
public final class Lookups {
    private static HolderLookup.Provider builtin;

    private Lookups() {
    }

    public static HolderLookup.Provider builtin() {
        if (builtin == null) builtin = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        return builtin;
    }
}
