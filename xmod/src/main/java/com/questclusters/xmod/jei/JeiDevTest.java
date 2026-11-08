package com.questclusters.xmod.jei;

import com.questclusters.client.DevSelfTest;
import com.questclusters.xmod.recipe.RecipeIndex;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.registry.ModDataComponents;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.IntSupplier;

/** JEI runtime checks: categories, crate entries, opening the recipe screen. Devtest only. */
public final class JeiDevTest {
    private JeiDevTest() {
    }

    private static void check(boolean ok, String what) {
        DevSelfTest.moduleCheck(ok, "xmod JEI: " + what);
    }

    public static List<IntSupplier> steps() {
        return List.of(
                () -> {
                    var r = QuestsJeiPlugin.runtime;
                    check(r != null, "runtime available");
                    if (r == null) {
                        return 1;
                    }
                    long quests = r.getRecipeManager().createRecipeLookup(JeiTypes.QUEST).get().count();
                    check(quests == RecipeIndex.INSTANCE.quests().size(), "quest category lists the book: " + quests);
                    long forDiamond = r.getRecipeManager().createRecipeLookup(JeiTypes.QUEST)
                            .limitFocus(List.of(r.getJeiHelpers().getFocusFactory().createFocus(
                                    mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, new ItemStack(Items.DIAMOND))))
                            .get().count();
                    check(forDiamond == 1, "recipe lookup for diamond: " + forDiamond);
                    long crates = r.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK).stream()
                            .filter(s -> "qc_test_crate".equals(s.get(ModDataComponents.LOOT_CRATE.get()))).count();
                    check(crates == 1, "the book's crate is one JEI entry: " + crates);
                    long categories = r.getRecipeManager().createRecipeCategoryLookup().includeHidden().get()
                            .filter(c -> c.getRecipeType().getUid().getNamespace().equals("ftbquests")).count();
                    check(categories == 2, "exactly two FTB Quests categories (no duplicates from XMod): " + categories);
                    FTBQuests.getRecipeModHelper().showRecipes(new ItemStack(Items.DIAMOND));
                    return 20;
                },
                () -> {
                    check(Minecraft.getInstance().screen != null
                            && Minecraft.getInstance().screen.getClass().getName().startsWith("mezz.jei"), "showRecipes opens JEI");
                    return 5;
                },
                () -> {
                    var r = QuestsJeiPlugin.runtime;
                    if (r != null) {
                        r.getRecipesGui().showTypes(List.of(JeiTypes.QUEST));
                    }
                    return 20;
                },
                () -> {
                    DevSelfTest.moduleShot("xmod_jei_quest");
                    var r = QuestsJeiPlugin.runtime;
                    if (r != null && !RecipeIndex.INSTANCE.crates().isEmpty()) {
                        r.getRecipesGui().showTypes(List.of(JeiTypes.LOOT_CRATE));
                    }
                    return 20;
                },
                () -> {
                    DevSelfTest.moduleShot("xmod_jei_crate");
                    Minecraft.getInstance().setScreen(null);
                    return 5;
                }
        );
    }
}
