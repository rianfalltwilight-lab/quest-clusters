package com.questclusters.xmod.jei;

import com.questclusters.xmod.QuestClustersXMod;
import com.questclusters.xmod.recipe.CrateRecipe;
import com.questclusters.xmod.recipe.QuestRecipe;
import com.questclusters.xmod.recipe.RecipeIndex;
import dev.ftb.mods.ftbquests.quest.loot.LootCrate;
import dev.ftb.mods.ftbquests.registry.ModDataComponents;
import dev.ftb.mods.ftbquests.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JEI side of the module: "Quests" and "Loot Crates" categories fed live from {@link RecipeIndex}, loot crates as
 * separate JEI ingredients, and the runtime that {@link JeiHelper} talks to.
 */
@JeiPlugin
public final class QuestsJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(QuestClustersXMod.MOD_ID, "quests");
    static @Nullable IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // every crate is its own JEI entry, told apart by its crate id
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModItems.LOOTCRATE.get(), new ISubtypeInterpreter<>() {
            @Override
            public @Nullable Object getSubtypeData(ItemStack stack, UidContext context) {
                return stack.get(ModDataComponents.LOOT_CRATE.get());
            }

            @Override
            @SuppressWarnings("deprecation")
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                String id = stack.get(ModDataComponents.LOOT_CRATE.get());
                return id == null ? "" : id;
            }
        });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new QuestCategory(registration.getJeiHelpers().getGuiHelper()), new CrateCategory());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(JeiTypes.QUEST, ModItems.BOOK.get());
        registration.addRecipeCatalysts(JeiTypes.LOOT_CRATE, ModItems.LOOTCRATE.get());
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addTypedRecipeManagerPlugin(JeiTypes.QUEST, new Quests());
        registration.addTypedRecipeManagerPlugin(JeiTypes.LOOT_CRATE, new Crates());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime r) {
        runtime = r;
        // JEI may (re)start after the book arrived: make sure the book's crates are listed
        Minecraft.getInstance().execute(() -> syncCrates(LootCrate.allCrateStacks(true)));
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /** Makes JEI's crate entries exactly {@code wanted}: crates of a previous book go, new ones come. */
    static void syncCrates(Collection<ItemStack> wanted) {
        IJeiRuntime r = runtime;
        if (r == null) {
            return;
        }
        IIngredientManager manager = r.getIngredientManager();
        List<ItemStack> present = manager.getAllIngredients(VanillaTypes.ITEM_STACK).stream()
                .filter(s -> s.has(ModDataComponents.LOOT_CRATE.get())).toList();
        List<ItemStack> remove = new ArrayList<>();
        for (ItemStack s : present) {
            if (wanted.stream().noneMatch(w -> ItemStack.isSameItemSameComponents(w, s))) {
                remove.add(s);
            }
        }
        List<ItemStack> add = new ArrayList<>();
        for (ItemStack w : wanted) {
            if (present.stream().noneMatch(s -> ItemStack.isSameItemSameComponents(w, s))) {
                add.add(w);
            }
        }
        if (!remove.isEmpty()) {
            manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, remove);
        }
        if (!add.isEmpty()) {
            manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, add);
        }
    }

    private static final class Quests implements ISimpleRecipeManagerPlugin<QuestRecipe> {
        @Override
        public boolean isHandledInput(ITypedIngredient<?> input) {
            return !getRecipesForInput(input).isEmpty();
        }

        @Override
        public boolean isHandledOutput(ITypedIngredient<?> output) {
            return !getRecipesForOutput(output).isEmpty();
        }

        @Override
        public List<QuestRecipe> getRecipesForInput(ITypedIngredient<?> input) {
            var item = input.getItemStack();
            if (item.isPresent()) {
                return RecipeIndex.INSTANCE.questsUsing(item.get());
            }
            return input.getIngredient(NeoForgeTypes.FLUID_STACK)
                    .map(fluid -> RecipeIndex.INSTANCE.questsUsing(fluid.getFluid())).orElse(List.of());
        }

        @Override
        public List<QuestRecipe> getRecipesForOutput(ITypedIngredient<?> output) {
            return output.getItemStack().map(RecipeIndex.INSTANCE::questsGiving).orElse(List.of());
        }

        @Override
        public List<QuestRecipe> getAllRecipes() {
            return RecipeIndex.INSTANCE.quests();
        }
    }

    private static final class Crates implements ISimpleRecipeManagerPlugin<CrateRecipe> {
        @Override
        public boolean isHandledInput(ITypedIngredient<?> input) {
            return !getRecipesForInput(input).isEmpty();
        }

        @Override
        public boolean isHandledOutput(ITypedIngredient<?> output) {
            return !getRecipesForOutput(output).isEmpty();
        }

        @Override
        public List<CrateRecipe> getRecipesForInput(ITypedIngredient<?> input) {
            return input.getItemStack().map(RecipeIndex.INSTANCE::cratesOf).orElse(List.of());
        }

        @Override
        public List<CrateRecipe> getRecipesForOutput(ITypedIngredient<?> output) {
            return output.getItemStack().map(RecipeIndex.INSTANCE::cratesGiving).orElse(List.of());
        }

        @Override
        public List<CrateRecipe> getAllRecipes() {
            return RecipeIndex.INSTANCE.crates();
        }
    }
}
