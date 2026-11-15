package com.questclusters.filters.filter;

import dev.ftb.mods.ftbfiltersystem.api.filter.SmartFilter;
import net.minecraft.core.HolderLookup;

/** FTB Filter System calls whose form differs between its 1.21.1 and 1.20.1 versions. */
public final class FfsCompat {
    private FfsCompat() {
    }

    /** The filter as a string, e.g. "or(item(...)...)". */
    public static String asString(SmartFilter filter, HolderLookup.Provider registries) {
        return filter.toString();
    }
}
