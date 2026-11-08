package com.questclusters.filters.filter;

import dev.ftb.mods.ftbfiltersystem.api.FilterException;
import dev.ftb.mods.ftbfiltersystem.api.filter.AbstractSmartFilter;
import dev.ftb.mods.ftbfiltersystem.api.filter.SmartFilter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Several item tags ({@code item_tag}) or block tags ({@code block_tag}); matches if any one matches. */
public abstract class TagListFilter extends AbstractSmartFilter {
    public static final int MAX_TAGS = 64;
    protected final List<ResourceLocation> tags;

    protected TagListFilter(@Nullable SmartFilter.Compound parent, List<ResourceLocation> tags) {
        super(parent);
        this.tags = List.copyOf(tags);
    }

    public List<ResourceLocation> tags() {
        return tags;
    }

    @Override
    public String getStringArg(HolderLookup.Provider registryAccess) {
        return String.join(",", tags.stream().map(ResourceLocation::toString).toList());
    }

    @Override
    public Component getDisplayArg(HolderLookup.Provider registryAccess) {
        if (tags.isEmpty()) return Component.translatable("quest_clusters_filters.tags.none");
        String first = "#" + tags.getFirst();
        return tags.size() == 1 ? Component.literal(first)
                : Component.translatable("quest_clusters_filters.tags.more", first, tags.size() - 1);
    }

    static List<ResourceLocation> parse(String arg) {
        List<ResourceLocation> out = new ArrayList<>();
        for (String part : ListArgs.split(arg)) {
            String t = ListArgs.normaliseTag(part);
            if (t == null) throw new FilterException("invalid tag: " + part);
            ResourceLocation rl = ResourceLocation.parse(t);
            if (!out.contains(rl)) out.add(rl);
        }
        return out;
    }

    public static final class Items extends TagListFilter {
        private final List<TagKey<Item>> keys;

        public Items(@Nullable SmartFilter.Compound parent) {
            this(parent, List.of());
        }

        public Items(@Nullable SmartFilter.Compound parent, List<ResourceLocation> tags) {
            super(parent, tags);
            this.keys = tags.stream().map(t -> TagKey.create(Registries.ITEM, t)).toList();
        }

        @Override
        public ResourceLocation getId() {
            return FilterIds.ITEM_TAGS;
        }

        @Override
        public boolean test(ItemStack stack) {
            for (TagKey<Item> k : keys) if (stack.is(k)) return true;
            return false;
        }

        public static Items fromString(SmartFilter.Compound parent, String arg, HolderLookup.Provider registries) {
            return new Items(parent, parse(arg));
        }
    }

    /** Matches items that place a block in one of the tags (seeds place crops, redstone places wire). */
    public static final class Blocks extends TagListFilter {
        private final List<TagKey<Block>> keys;

        public Blocks(@Nullable SmartFilter.Compound parent) {
            this(parent, List.of());
        }

        public Blocks(@Nullable SmartFilter.Compound parent, List<ResourceLocation> tags) {
            super(parent, tags);
            this.keys = tags.stream().map(t -> TagKey.create(Registries.BLOCK, t)).toList();
        }

        @Override
        public ResourceLocation getId() {
            return FilterIds.BLOCK_TAGS;
        }

        @Override
        public boolean test(ItemStack stack) {
            if (!(stack.getItem() instanceof BlockItem bi)) return false;
            var state = bi.getBlock().defaultBlockState();
            for (TagKey<Block> k : keys) if (state.is(k)) return true;
            return false;
        }

        public static Blocks fromString(SmartFilter.Compound parent, String arg, HolderLookup.Provider registries) {
            return new Blocks(parent, parse(arg));
        }
    }
}
