package com.questclusters.filters.filter;

import dev.ftb.mods.ftbfiltersystem.api.FilterException;
import dev.ftb.mods.ftbfiltersystem.api.filter.AbstractSmartFilter;
import dev.ftb.mods.ftbfiltersystem.api.filter.SmartFilter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code smart_filter_extras:items(a,b,...)}: matches any listed item. An entry is an item id, or a variant written
 * like {@code /give} that also needs that data ({@code minecraft:potion[minecraft:potion_contents={...}]} on 1.21.1,
 * {@code minecraft:potion{Potion:"..."}} on 1.20.1; see ItemVariant). Ids of items that do not exist (a removed
 * mod) are kept but match nothing.
 */
public class ItemListFilter extends AbstractSmartFilter {
    public static final int MAX_ENTRIES = 256, MAX_ENTRY_LENGTH = 2048;

    /** @param text normalised entry text (id, or id with its variant data) */
    public record Entry(String text, @Nullable Item item, @Nullable ItemVariant data) {
        public boolean missing() {
            return item == null;
        }

        public boolean variant() {
            return data != null && !data.isEmpty();
        }

        public boolean test(ItemStack stack) {
            if (item == null || !stack.is(item)) return false;
            return !variant() || data.matches(stack);
        }
    }

    private final List<Entry> entries;

    public ItemListFilter(@Nullable SmartFilter.Compound parent) {
        this(parent, List.of());
    }

    public ItemListFilter(@Nullable SmartFilter.Compound parent, List<Entry> entries) {
        super(parent);
        this.entries = List.copyOf(entries);
    }

    public List<Entry> entries() {
        return entries;
    }

    @Override
    public ResourceLocation getId() {
        return FilterIds.ITEMS;
    }

    @Override
    public boolean test(ItemStack stack) {
        for (Entry e : entries) if (e.test(stack)) return true;
        return false;
    }

    @Override
    //? if neoforge {
    public String getStringArg(HolderLookup.Provider registryAccess) {
    //?} else {
    /*public String getStringArg() {
        HolderLookup.Provider registryAccess = Lookups.builtin();
    *///?}
        List<String> parts = new ArrayList<>();
        for (Entry e : entries) parts.add(ListArgs.encode(e.text()));
        return String.join(",", parts);
    }

    @Override
    //? if neoforge {
    public Component getDisplayArg(HolderLookup.Provider registryAccess) {
    //?} else {
    /*public Component getDisplayArg() {
        HolderLookup.Provider registryAccess = Lookups.builtin();
    *///?}
        List<Entry> found = entries.stream().filter(e -> !e.missing()).toList();
        if (entries.isEmpty()) return Component.translatable("quest_clusters_filters.items.none");
        Component a = found.isEmpty() ? Component.literal(entries.get(0).text()) : name(found.get(0));
        if (entries.size() == 1) return a;
        if (entries.size() == 2 && found.size() == 2) return Component.translatable("quest_clusters_filters.items.two", a, name(found.get(1)));
        Component b = found.size() > 1 ? name(found.get(1)) : Component.literal(entries.get(1).text());
        return entries.size() == 2 ? Component.translatable("quest_clusters_filters.items.two", a, b)
                : Component.translatable("quest_clusters_filters.items.more", a, b, entries.size() - 2);
    }

    private static Component name(Entry e) {
        return stackOf(e).getHoverName();
    }

    /** A display stack for an entry (empty for missing items). */
    public static ItemStack stackOf(Entry e) {
        if (e.item() == null) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(e.item());
        if (e.data() != null) e.data().applyTo(s);
        return s;
    }

    public static ItemListFilter fromString(SmartFilter.Compound parent, String arg, HolderLookup.Provider registries) {
        List<Entry> out = new ArrayList<>();
        for (String part : ListArgs.split(arg)) {
            Entry e = parseEntry(ListArgs.decode(part), registries);
            if (out.stream().noneMatch(x -> x.text().equals(e.text()))) out.add(e);
        }
        return new ItemListFilter(parent, out);
    }

    /** Parses one entry; unknown item ids become missing entries rather than errors. */
    public static Entry parseEntry(String raw, HolderLookup.Provider registries) {
        String s = raw.trim();
        int bracket = s.indexOf(ItemVariant.OPEN);
        if (bracket < 0) {
            ResourceLocation id = ResourceLocation.tryParse(s.indexOf(':') < 0 ? "minecraft:" + s : s);
            if (id == null) throw new FilterException("invalid item id: " + raw);
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            return new Entry(id.toString(), item, null);
        }
        String idPart = s.substring(0, bracket).trim();
        ResourceLocation id = ResourceLocation.tryParse(idPart.indexOf(':') < 0 ? "minecraft:" + idPart : idPart);
        if (id == null) throw new FilterException("invalid item id: " + raw);
        if (BuiltInRegistries.ITEM.getOptional(id).isEmpty()) {
            return new Entry(id + s.substring(bracket), null, null);
        }
        try {
            var result = ItemVariant.parse(id + s.substring(bracket), registries);
            ItemVariant data = result.variant();
            return new Entry(textOf(result.item(), data, registries), result.item(), data.isEmpty() ? null : data);
        } catch (Exception ex) {
            throw new FilterException("invalid item variant: " + raw + " (" + ex.getMessage() + ")");
        }
    }

    /** Canonical entry text: the id, plus the variant data the way /give writes it. */
    public static String textOf(Item item, @Nullable ItemVariant data, HolderLookup.Provider registries) {
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        return data == null ? id : id + data.text(registries);
    }
}
