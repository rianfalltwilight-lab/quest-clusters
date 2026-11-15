package com.questclusters.filters.filter;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * What makes an item list entry a variant (Minecraft 1.20.1: the item's NBT, written like {@code /give}:
 * {@code minecraft:potion{Potion:"minecraft:healing"}}). A stack matches if it has at least that NBT.
 */
public final class ItemVariant {
    /** Where the data starts in an entry, after the item id. */
    public static final char OPEN = '{';
    /** FTB Filter System's own filter for item data (used when rewriting item lists without this module). */
    public static final String NATIVE_DATA_FILTER = "nbt";

    /** Data that changes with use or is stored content: not part of what "kind" of item a stack is. */
    private static final Set<String> NOT_VARIANT = Set.of("Damage", "RepairCost", "ChargedProjectiles", "pages",
            "BlockEntityTag", "Items", "LootTable", "LootTableSeed", "DebugProperty");

    private final CompoundTag tag;

    private ItemVariant(CompoundTag tag) {
        this.tag = tag;
    }

    public record Parsed(Item item, ItemVariant variant) {
    }

    /** Parses "id{...}" (the id is known to exist). */
    public static Parsed parse(String text, HolderLookup.Provider registries) throws Exception {
        var result = ItemParser.parseForItem(BuiltInRegistries.ITEM.asLookup(), new StringReader(text));
        return new Parsed(result.item().value(), new ItemVariant(result.nbt() == null ? new CompoundTag() : result.nbt()));
    }

    /** The kind of item a stack is (without wear, contents and the like). */
    public static ItemVariant of(ItemStack stack) {
        CompoundTag t = stack.getTag() == null ? new CompoundTag() : stack.getTag().copy();
        NOT_VARIANT.forEach(t::remove);
        return new ItemVariant(t);
    }

    public boolean isEmpty() {
        return tag.isEmpty();
    }

    public boolean matches(ItemStack stack) {
        return NbtUtils.compareNbt(tag, stack.getTag(), true);
    }

    public void applyTo(ItemStack stack) {
        if (!tag.isEmpty()) stack.getOrCreateTag().merge(tag.copy());
    }

    /** The data as written after the id ("{...}"), or "" for none. */
    public String text(HolderLookup.Provider registries) {
        return tag.isEmpty() ? "" : tag.toString();
    }

    /** The data as SNBT for FTB Filter System's own data filter, or null if it cannot be written. */
    public String nativeData(HolderLookup.Provider registries) {
        return tag.toString();
    }
}
