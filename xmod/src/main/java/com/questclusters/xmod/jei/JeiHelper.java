package com.questclusters.xmod.jei;

import com.questclusters.xmod.QuestClustersXMod;
import com.questclusters.xmod.recipe.ViewerHelper;
import dev.architectury.fluid.FluidStack;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.config.Tristate;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
//? if neoforge {
import mezz.jei.api.runtime.IBookmarkManager;
//?}
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

/** FTB Quests' recipe-viewer hook backed by JEI: show recipes, bookmarks, crate entries. */
public final class JeiHelper extends ViewerHelper {
    /** Too Many Recipe Viewers runs JEI plugins on EMI and has no JEI bookmark list. */
    private final boolean bookmarks = !QuestClustersXMod.loaded("toomanyrecipeviewers");

    @Override
    public String getHelperName() {
        return "JEI";
    }

    @Override
    public void showRecipes(ItemStack stack) {
        show(VanillaTypes.ITEM_STACK, stack);
    }

    // FTB Quests 1.21.1 only (no @Override: 1.20.1 has no such hook)
    public void showRecipes(FluidStack fluid) {
        //? if neoforge {
        show(NeoForgeTypes.FLUID_STACK, new net.neoforged.neoforge.fluids.FluidStack(
                fluid.getFluid().builtInRegistryHolder(), (int) Math.min(Integer.MAX_VALUE, Math.max(1, fluid.getAmount())), fluid.getPatch()));
        //?} else {
        /*show(ForgeTypes.FLUID_STACK, new net.minecraftforge.fluids.FluidStack(
                fluid.getFluid(), (int) Math.min(Integer.MAX_VALUE, Math.max(1, fluid.getAmount())), fluid.getTag()));
        *///?}
    }

    private static <T> void show(IIngredientType<T> type, T ingredient) {
        IJeiRuntime r = QuestsJeiPlugin.runtime;
        if (r != null) {
            //? if neoforge {
            r.getIngredientManager().createTypedIngredient(type, ingredient, false).ifPresent(typed -> r.getRecipesGui()
            //?} else {
            /*r.getIngredientManager().createTypedIngredient(type, ingredient).ifPresent(typed -> r.getRecipesGui()
            *///?}
                    .show(r.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, typed)));
        }
    }

    // FTB Quests 1.21.1 only (no @Override: 1.20.1 has no such hook)
    public Tristate toggleBookmark(ItemStack stack) {
        IJeiRuntime r = QuestsJeiPlugin.runtime;
        if (r == null || !bookmarks) {
            return Tristate.DEFAULT;
        }
        //? if neoforge {
        return r.getIngredientManager().createTypedIngredient(VanillaTypes.ITEM_STACK, stack, true).map(typed -> {
            IBookmarkManager manager = r.getBookmarkManager();
            if (manager.contains(typed)) {
                return manager.remove(typed) ? Tristate.FALSE : Tristate.DEFAULT;
            }
            return manager.add(typed) ? Tristate.TRUE : Tristate.DEFAULT;
        }).orElse(Tristate.DEFAULT);
        //?} else {
        /*return Tristate.DEFAULT; // JEI 15 (1.20.1) has no bookmark API: FTB Quests' own handling stays
        *///?}
    }

    // FTB Quests 1.21.1 only (no @Override: 1.20.1 has no such hook)
    public boolean isBookmarkKey(Key key) {
        IJeiRuntime r = QuestsJeiPlugin.runtime;
        //? if neoforge {
        return r != null && bookmarks && r.getKeyMappings().getBookmark().isActiveAndMatches(key.getInputMapping());
        //?} else {
        /*return false;
        *///?}
    }

    @Override
    public void updateItemsDynamic(Collection<ItemStack> toRemove, Collection<ItemStack> toAdd) {
        QuestsJeiPlugin.syncCrates(toAdd);
    }
}
