package com.questclusters.xmod.rei;

import com.questclusters.xmod.recipe.QuestRecipe;
import com.questclusters.xmod.recipe.RecipeIndex;
import dev.architectury.fluid.FluidStack;
import dev.ftb.mods.ftbquests.registry.ModDataComponents;
import dev.ftb.mods.ftbquests.registry.ModItems;
import me.shedaniel.rei.api.client.entry.filtering.base.BasicFilteringRule;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** REI side of the module; displays are generated on demand from {@link RecipeIndex}. */
@REIPluginClient
public final class QuestsReiPlugin implements REIClientPlugin {
    private static BasicFilteringRule.MarkDirty cratesShown;

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new ReiCategories.Quest(), new ReiCategories.Crate());
        registry.addWorkstations(ReiDisplays.QUEST, EntryStacks.of(ModItems.BOOK.get()));
        registry.addWorkstations(ReiDisplays.LOOT_CRATE, EntryStacks.of(ModItems.LOOTCRATE.get()));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        registry.registerDisplayGenerator(ReiDisplays.QUEST, new Generator<>(ReiDisplays.QUEST, ReiDisplays.QuestDisplay::new,
                RecipeIndex.INSTANCE::quests, RecipeIndex.INSTANCE::questsGiving, QuestsReiPlugin::questsUsing));
        registry.registerDisplayGenerator(ReiDisplays.LOOT_CRATE, new Generator<>(ReiDisplays.LOOT_CRATE, ReiDisplays.CrateDisplay::new,
                RecipeIndex.INSTANCE::crates, RecipeIndex.INSTANCE::cratesGiving, e -> e.getType() == VanillaEntryTypes.ITEM
                ? RecipeIndex.INSTANCE.cratesOf(e.castValue()) : List.of()));
    }

    private static List<QuestRecipe> questsUsing(EntryStack<?> entry) {
        if (entry.getType() == VanillaEntryTypes.ITEM) {
            return RecipeIndex.INSTANCE.questsUsing((ItemStack) entry.castValue());
        }
        if (entry.getType() == VanillaEntryTypes.FLUID) {
            return RecipeIndex.INSTANCE.questsUsing(((FluidStack) entry.castValue()).getFluid());
        }
        return List.of();
    }

    @Override
    public void registerBasicEntryFiltering(BasicFilteringRule<?> rule) {
        cratesShown = rule.show(() -> RecipeIndex.INSTANCE.crateStacks().stream().<EntryStack<?>>map(EntryStacks::of).toList());
    }

    /** Swaps the crate entries in REI's list for {@code wanted} (FTB Quests calls this when the book's crates change). */
    static void syncCrates(Collection<ItemStack> wanted) {
        EntryRegistry entries = EntryRegistry.getInstance();
        entries.removeEntryIf(e -> e.getType() == VanillaEntryTypes.ITEM && ((ItemStack) e.castValue()).has(ModDataComponents.LOOT_CRATE.get()));
        entries.addEntries(wanted.stream().<EntryStack<?>>map(EntryStacks::of).toList());
        if (cratesShown != null) {
            cratesShown.markDirty();
        }
        entries.refilter();
    }

    /** Displays for one category; wrappers are reused until the index changes. */
    private static final class Generator<R, D extends me.shedaniel.rei.api.common.display.Display> implements DynamicDisplayGenerator<D> {
        private final CategoryIdentifier<D> category;
        private final Function<R, D> wrap;
        private final java.util.function.Supplier<List<R>> all;
        private final Function<ItemStack, List<R>> giving;
        private final Function<EntryStack<?>, List<R>> using;
        private final Map<R, D> cache = new IdentityHashMap<>();
        private int generation = -1;

        Generator(CategoryIdentifier<D> category, Function<R, D> wrap, java.util.function.Supplier<List<R>> all,
                  Function<ItemStack, List<R>> giving, Function<EntryStack<?>, List<R>> using) {
            this.category = category;
            this.wrap = wrap;
            this.all = all;
            this.giving = giving;
            this.using = using;
        }

        private List<D> wrap(List<R> recipes) {
            if (generation != RecipeIndex.INSTANCE.generation()) {
                generation = RecipeIndex.INSTANCE.generation();
                cache.clear();
            }
            return recipes.stream().map(r -> cache.computeIfAbsent(r, wrap)).toList();
        }

        private Optional<List<D>> nonEmpty(List<D> list) {
            return list.isEmpty() ? Optional.empty() : Optional.of(list);
        }

        @Override
        public Optional<List<D>> getRecipeFor(EntryStack<?> entry) {
            return entry.getType() == VanillaEntryTypes.ITEM ? nonEmpty(wrap(giving.apply(entry.castValue()))) : Optional.empty();
        }

        @Override
        public Optional<List<D>> getUsageFor(EntryStack<?> entry) {
            return nonEmpty(wrap(using.apply(entry)));
        }

        @Override
        public Optional<List<D>> generate(ViewSearchBuilder builder) {
            return builder.getCategories().contains(category) ? nonEmpty(wrap(all.get())) : Optional.empty();
        }
    }
}
