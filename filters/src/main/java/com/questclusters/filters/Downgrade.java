package com.questclusters.filters;

import com.questclusters.filters.filter.FuelFilter;
import com.questclusters.filters.filter.ItemListFilter;
import com.questclusters.filters.filter.TagListFilter;
import dev.ftb.mods.ftbfiltersystem.api.FTBFilterSystemAPI;
import dev.ftb.mods.ftbfiltersystem.api.filter.SmartFilter;
import net.minecraft.core.HolderLookup;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewrites a filter tree with FTB Filter System's own filters where that keeps the same matches, so the filter
 * survives removing this module: item lists become item / or(item ...) / and(item component(fuzzy:...)),
 * item tag lists become item_tag / or(item_tag ...). Block tags and fuel value have no equivalent and stay.
 */
public final class Downgrade {
    public static final int MAX_LENGTH = 32767;

    public static final class Report {
        public int converted, droppedMissing, keptNeedsModule, unconvertibleVariants;
    }

    private Downgrade() {
    }

    public static String rewrite(SmartFilter filter, HolderLookup.Provider registries, Report report) {
        if (filter instanceof ItemListFilter list) return items(list, registries, report);
        if (filter instanceof TagListFilter.Items tags) {
            report.converted++;
            List<String> parts = tags.tags().stream().map(t -> "item_tag(" + t + ")").toList();
            return parts.size() == 1 ? parts.get(0) : "or(" + String.join("", parts) + ")";
        }
        if (filter instanceof TagListFilter.Blocks || filter instanceof FuelFilter) {
            report.keptNeedsModule++;
            return com.questclusters.filters.filter.FfsCompat.asString(filter, registries);
        }
        if (filter instanceof SmartFilter.Compound c) {
            StringBuilder inner = new StringBuilder();
            for (SmartFilter child : c.getChildren()) inner.append(rewrite(child, registries, report));
            if (c.getParent() == null) return inner.toString(); // the root is just its children
            return FTBFilterSystemAPI.modDefaultedString(c.getId()) + "(" + inner + ")";
        }
        return com.questclusters.filters.filter.FfsCompat.asString(filter, registries);
    }

    private static String items(ItemListFilter list, HolderLookup.Provider registries, Report report) {
        List<String> parts = new ArrayList<>();
        for (ItemListFilter.Entry e : list.entries()) {
            if (e.missing()) {
                report.droppedMissing++;
                continue;
            }
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.item()).toString();
            if (!e.variant()) {
                parts.add("item(" + id + ")");
                continue;
            }
            String nbt = e.data().nativeData(registries);
            if (nbt == null || nbt.indexOf('(') >= 0 || nbt.indexOf(')') >= 0 || nbt.indexOf('\\') >= 0) {
                // FTB Filter System's component filter cannot store these characters reliably: keep the list as it is
                report.unconvertibleVariants++;
                report.keptNeedsModule++;
                return com.questclusters.filters.filter.FfsCompat.asString(list, registries);
            }
            parts.add("and(item(" + id + ")" + com.questclusters.filters.filter.ItemVariant.NATIVE_DATA_FILTER + "(fuzzy:" + nbt + "))");
        }
        report.converted++;
        if (parts.isEmpty()) return "";
        return parts.size() == 1 ? parts.get(0) : "or(" + String.join("", parts) + ")";
    }
}
