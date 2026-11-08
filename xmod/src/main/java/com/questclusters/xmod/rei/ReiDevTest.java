package com.questclusters.xmod.rei;

import com.questclusters.client.DevSelfTest;
import com.questclusters.xmod.recipe.RecipeIndex;
import dev.ftb.mods.ftbquests.FTBQuests;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.IntSupplier;

/** REI checks (run with -PwithRei). Devtest only. */
public final class ReiDevTest {
    private ReiDevTest() {
    }

    private static void check(boolean ok, String what) {
        DevSelfTest.moduleCheck(ok, "xmod REI: " + what);
    }

    public static List<IntSupplier> steps() {
        return List.of(
                () -> {
                    check(FTBQuests.getRecipeModHelper() instanceof ReiHelper, "FTB Quests uses the REI helper");
                    long forDiamond = ViewSearchBuilder.builder().addRecipesFor(EntryStacks.of(new ItemStack(Items.DIAMOND)))
                            .streamDisplays().filter(d -> d.provideInternalDisplay() instanceof ReiDisplays.QuestDisplay).count();
                    check(forDiamond == 1, "quest display for diamond: " + forDiamond);
                    long crates = ViewSearchBuilder.builder().addCategory(ReiDisplays.LOOT_CRATE)
                            .streamDisplays().count();
                    check(crates == RecipeIndex.INSTANCE.crates().size(), "loot crate displays: " + crates);
                    FTBQuests.getRecipeModHelper().showRecipes(new ItemStack(Items.DIAMOND));
                    return 30;
                },
                () -> {
                    check(Minecraft.getInstance().screen != null
                            && Minecraft.getInstance().screen.getClass().getName().startsWith("me.shedaniel"), "showRecipes opens REI");
                    return 5;
                },
                () -> {
                    ViewSearchBuilder.builder().addRecipesFor(EntryStacks.of(new ItemStack(Items.DIAMOND)))
                            .setPreferredOpenedCategory(ReiDisplays.QUEST).open();
                    return 30;
                },
                () -> {
                    DevSelfTest.moduleShot("xmod_rei_quest");
                    ViewSearchBuilder.builder().addCategory(ReiDisplays.LOOT_CRATE).open();
                    return 30;
                },
                () -> {
                    DevSelfTest.moduleShot("xmod_rei_crate");
                    Minecraft.getInstance().setScreen(null);
                    return 5;
                }
        );
    }
}
