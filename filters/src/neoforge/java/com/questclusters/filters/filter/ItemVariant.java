package com.questclusters.filters.filter;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What makes an item list entry a variant (Minecraft 1.21: data components, written like {@code /give}:
 * {@code minecraft:potion[minecraft:potion_contents={potion:"minecraft:healing"}]}).
 */
public final class ItemVariant {
    /** Where the data starts in an entry, after the item id. */
    public static final char OPEN = '[';
    /** FTB Filter System's own filter for item data (used when rewriting item lists without this module). */
    public static final String NATIVE_DATA_FILTER = "component";

    /** Data that changes with use or is stored content: not part of what "kind" of item a stack is. */
    private static final Set<DataComponentType<?>> NOT_VARIANT = Set.of(DataComponents.DAMAGE, DataComponents.REPAIR_COST,
            DataComponents.CHARGED_PROJECTILES, DataComponents.WRITABLE_BOOK_CONTENT, DataComponents.CONTAINER,
            DataComponents.BUNDLE_CONTENTS, DataComponents.BLOCK_ENTITY_DATA, DataComponents.BEES,
            DataComponents.CONTAINER_LOOT, DataComponents.DEBUG_STICK_STATE);

    private final DataComponentPatch patch;

    private ItemVariant(DataComponentPatch patch) {
        this.patch = patch;
    }

    public record Parsed(Item item, ItemVariant variant) {
    }

    /** Parses "id[...]" (the id is known to exist). */
    public static Parsed parse(String text, HolderLookup.Provider registries) throws Exception {
        var result = new ItemParser(registries).parse(new StringReader(text));
        return new Parsed(result.item().value(), new ItemVariant(result.components()));
    }

    /** The kind of item a stack is (without wear, contents and the like). */
    public static ItemVariant of(ItemStack stack) {
        DataComponentPatch.Builder b = DataComponentPatch.builder();
        for (Map.Entry<DataComponentType<?>, Optional<?>> e : stack.getComponentsPatch().entrySet()) {
            if (NOT_VARIANT.contains(e.getKey()) || e.getValue().isEmpty()) continue;
            set(b, e.getKey(), e.getValue().get());
        }
        return new ItemVariant(b.build());
    }

    @SuppressWarnings("unchecked")
    private static <T> void set(DataComponentPatch.Builder b, DataComponentType<T> type, Object v) {
        b.set(type, (T) v);
    }

    public boolean isEmpty() {
        return patch.isEmpty();
    }

    public boolean matches(ItemStack stack) {
        for (Map.Entry<DataComponentType<?>, Optional<?>> e : patch.entrySet()) {
            Object have = stack.getComponents().get(e.getKey());
            if (e.getValue().isPresent() ? !Objects.equals(have, e.getValue().get()) : have != null) return false;
        }
        return true;
    }

    public void applyTo(ItemStack stack) {
        stack.applyComponents(patch);
    }

    /** The data as written after the id ("[a=...,b=...]", sorted), or "" for none. */
    public String text(HolderLookup.Provider registries) {
        if (patch.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
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
        return "[" + String.join(",", parts) + "]";
    }

    @SuppressWarnings("unchecked")
    private static <T> String encodeValue(DataComponentType<T> type, Object value, com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops) {
        if (type.codec() == null) return null;
        return type.codec().encodeStart(ops, (T) value).result().map(net.minecraft.nbt.Tag::toString).orElse(null);
    }

    /** The data as SNBT for FTB Filter System's own data filter, or null if it cannot be written. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public String nativeData(HolderLookup.Provider registries) {
        DataComponentMap.Builder b = DataComponentMap.builder();
        for (Map.Entry<DataComponentType<?>, Optional<?>> c : patch.entrySet()) {
            if (c.getValue().isPresent()) b.set((DataComponentType) c.getKey(), c.getValue().get());
        }
        return DataComponentMap.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), b.build())
                .result().map(Object::toString).orElse(null);
    }
}
