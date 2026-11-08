package com.questclusters.filters.filter;

import com.mojang.brigadier.StringReader;
import dev.ftb.mods.ftbfiltersystem.api.FilterException;
import dev.ftb.mods.ftbfiltersystem.api.filter.AbstractSmartFilter;
import dev.ftb.mods.ftbfiltersystem.api.filter.SmartFilter;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * {@code smart_filter_extras:items(a,b,...)}: matches any listed item. An entry is an item id, or a variant written
 * like {@code /give} ({@code minecraft:potion[minecraft:potion_contents={...}]}) that also needs those components.
 * Ids of items that do not exist (a removed mod) are kept but match nothing.
 */
public class ItemListFilter extends AbstractSmartFilter {
    public static final int MAX_ENTRIES = 256, MAX_ENTRY_LENGTH = 2048;

    /** @param text normalised entry text (id, or id[components]) */
    public record Entry(String text, @Nullable Item item, @Nullable DataComponentPatch components) {
        public boolean missing() {
            return item == null;
        }

        public boolean variant() {
            return components != null && !components.isEmpty();
        }

        public boolean test(ItemStack stack) {
            if (item == null || !stack.is(item)) return false;
            if (!variant()) return true;
            for (Map.Entry<DataComponentType<?>, Optional<?>> e : components.entrySet()) {
                Object have = stack.getComponents().get(e.getKey());
                if (e.getValue().isPresent() ? !Objects.equals(have, e.getValue().get()) : have != null) return false;
            }
            return true;
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
    public String getStringArg(HolderLookup.Provider registryAccess) {
        List<String> parts = new ArrayList<>();
        for (Entry e : entries) parts.add(ListArgs.encode(e.text()));
        return String.join(",", parts);
    }

    @Override
    public Component getDisplayArg(HolderLookup.Provider registryAccess) {
        List<Entry> found = entries.stream().filter(e -> !e.missing()).toList();
        if (entries.isEmpty()) return Component.translatable("quest_clusters_filters.items.none");
        Component a = found.isEmpty() ? Component.literal(entries.getFirst().text()) : name(found.get(0));
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
        if (e.components() != null) s.applyComponents(e.components());
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
        int bracket = s.indexOf('[');
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
            var result = new ItemParser(registries).parse(new StringReader(id + s.substring(bracket)));
            DataComponentPatch patch = result.components();
            return new Entry(textOf(result.item().value(), patch, registries), result.item().value(), patch.isEmpty() ? null : patch);
        } catch (Exception ex) {
            throw new FilterException("invalid item variant: " + raw + " (" + ex.getMessage() + ")");
        }
    }

    /** Canonical entry text: the id, plus "[...]" with the component patch the way /give writes it. */
    public static String textOf(Item item, @Nullable DataComponentPatch patch, HolderLookup.Provider registries) {
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        if (patch == null || patch.isEmpty()) return id;
        List<String> parts = new ArrayList<>();
        var ops = registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        for (Map.Entry<DataComponentType<?>, Optional<?>> e : patch.entrySet()) {
            ResourceLocation key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(e.getKey());
            if (key == null) continue;
            if (e.getValue().isEmpty()) {
                parts.add("!" + key);
                continue;
            }
            String value = encodeValue(e.getKey(), e.getValue().get(), ops);
            if (value != null) parts.add(key + "=" + value);
        }
        parts.sort(String::compareTo);
        return id + "[" + String.join(",", parts) + "]";
    }

    @SuppressWarnings("unchecked")
    private static <T> String encodeValue(DataComponentType<T> type, Object value, com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops) {
        if (type.codec() == null) return null;
        return type.codec().encodeStart(ops, (T) value).result().map(net.minecraft.nbt.Tag::toString).orElse(null);
    }
}
