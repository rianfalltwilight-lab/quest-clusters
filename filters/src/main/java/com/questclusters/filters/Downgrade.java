package com.questclusters.filters;

import com.questclusters.filters.filter.FuelFilter;
import com.questclusters.filters.filter.ItemListFilter;
import com.questclusters.filters.filter.TagListFilter;
import dev.ftb.mods.ftbfiltersystem.api.FTBFilterSystemAPI;
import dev.ftb.mods.ftbfiltersystem.api.filter.SmartFilter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.NbtOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            return parts.size() == 1 ? parts.getFirst() : "or(" + String.join("", parts) + ")";
        }
        if (filter instanceof TagListFilter.Blocks || filter instanceof FuelFilter) {
            report.keptNeedsModule++;
            return filter.asString(registries);
        }
        if (filter instanceof SmartFilter.Compound c) {
            StringBuilder inner = new StringBuilder();
            for (SmartFilter child : c.getChildren()) inner.append(rewrite(child, registries, report));
            if (c.getParent() == null) return inner.toString(); // the root is just its children
            return FTBFilterSystemAPI.modDefaultedString(c.getId()) + "(" + inner + ")";
        }
        return filter.asString(registries);
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
            String nbt = componentNbt(e, registries);
            if (nbt == null || nbt.indexOf('(') >= 0 || nbt.indexOf(')') >= 0 || nbt.indexOf('\\') >= 0) {
                // FTB Filter System's component filter cannot store these characters reliably: keep the list as it is
                report.unconvertibleVariants++;
                report.keptNeedsModule++;
                return list.asString(registries);
            }
            parts.add("and(item(" + id + ")component(fuzzy:" + nbt + "))");
        }
        report.converted++;
        if (parts.isEmpty()) return "";
        return parts.size() == 1 ? parts.getFirst() : "or(" + String.join("", parts) + ")";
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String componentNbt(ItemListFilter.Entry e, HolderLookup.Provider registries) {
        DataComponentMap.Builder b = DataComponentMap.builder();
        for (Map.Entry<DataComponentType<?>, Optional<?>> c : e.components().entrySet()) {
            if (c.getValue().isPresent()) b.set((DataComponentType) c.getKey(), c.getValue().get());
        }
        return DataComponentMap.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), b.build())
                .result().map(Object::toString).orElse(null);
    }
}
