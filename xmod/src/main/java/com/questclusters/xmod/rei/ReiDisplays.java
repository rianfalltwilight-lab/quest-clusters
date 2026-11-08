package com.questclusters.xmod.rei;

import com.questclusters.xmod.recipe.CrateRecipe;
import com.questclusters.xmod.recipe.QuestRecipe;
import dev.architectury.fluid.FluidStack;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/** REI displays wrapping the shared recipes. Ids match FTB XMod Compat's, so REI settings carry over. */
final class ReiDisplays {
    static final CategoryIdentifier<QuestDisplay> QUEST = CategoryIdentifier.of("ftbquests", "quest");
    static final CategoryIdentifier<CrateDisplay> LOOT_CRATE = CategoryIdentifier.of("ftbquests", "loot_crate");

    private ReiDisplays() {
    }

    static EntryIngredient slot(QuestRecipe.Slot slot) {
        EntryIngredient entries = slot.isFluid()
                ? EntryIngredient.of(EntryStacks.of(FluidStack.create(slot.fluid(), slot.fluidAmount(), slot.fluidComponents())))
                : EntryIngredients.ofItemStacks(slot.items());
        if (slot.overflow() > 0) {
            Component more = Component.translatable("quest_clusters_xmod.recipe.more", slot.overflow()).withStyle(ChatFormatting.GRAY);
            entries = entries.map(e -> e.copy().tooltip(more));
        }
        return entries;
    }

    record QuestDisplay(QuestRecipe recipe, List<EntryIngredient> inputs, List<EntryIngredient> outputs) implements Display {
        QuestDisplay(QuestRecipe recipe) {
            this(recipe, recipe.inputs().stream().map(ReiDisplays::slot).toList(), recipe.outputs().stream().map(ReiDisplays::slot).toList());
        }

        @Override
        public List<EntryIngredient> getInputEntries() {
            return inputs;
        }

        @Override
        public List<EntryIngredient> getOutputEntries() {
            return outputs;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return QUEST;
        }
    }

    record CrateDisplay(CrateRecipe recipe, List<EntryIngredient> outputs) implements Display {
        CrateDisplay(CrateRecipe recipe) {
            this(recipe, recipe.slots().stream().map(drops -> EntryIngredient.of(drops.stream()
                    .map(d -> EntryStacks.of(d.stack()).tooltip(Component.translatable("jei.ftbquests.lootcrates.chance",
                            Component.literal(recipe.chance(d)).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY)))
                    .toList())).toList());
        }

        @Override
        public List<EntryIngredient> getInputEntries() {
            return List.of(EntryIngredients.of(recipe.crateStack()));
        }

        @Override
        public List<EntryIngredient> getOutputEntries() {
            return outputs;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return LOOT_CRATE;
        }
    }
}
