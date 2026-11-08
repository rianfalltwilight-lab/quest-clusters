package com.questclusters.xmod.client;

import com.questclusters.client.DevSelfTest;
import com.questclusters.xmod.recipe.CrateRecipe;
import com.questclusters.xmod.recipe.QuestRecipe;
import com.questclusters.xmod.recipe.RecipeIndex;
import dev.ftb.mods.ftbquests.FTBQuests;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/** Recipe-viewer checks on the devtest chapter "recipe_test"; JEI-specific ones run with -PwithJei. */
final class RecipeDevTest {
    private static final long RECIPE = 0xA20L, HIDDEN = 0xA21L, LOCKED = 0xA22L;

    private RecipeDevTest() {
    }

    private static void check(boolean ok, String what) {
        DevSelfTest.moduleCheck(ok, "xmod recipes: " + what);
    }

    private static boolean has(List<QuestRecipe> list, long id) {
        return list.stream().anyMatch(r -> r.quest().id == id);
    }

    static List<IntSupplier> steps() {
        List<IntSupplier> steps = new ArrayList<>();
        steps.add(() -> {
            RecipeIndex.INSTANCE.invalidateAll();
            List<QuestRecipe> quests = RecipeIndex.INSTANCE.quests();
            check(has(quests, RECIPE), "quest with item rewards is a recipe");
            check(!has(quests, HIDDEN), "\"disable in recipe viewers\" is respected");
            check(!has(quests, LOCKED), "quests the team cannot start yet are not shown");
            QuestRecipe r = quests.stream().filter(q -> q.quest().id == RECIPE).findFirst().orElse(null);
            if (r != null) {
                check(r.inputs().size() == 2 && r.inputs().get(1).isFluid() && r.inputs().get(1).fluidAmount() == 1000,
                        "inputs: item task + fluid task (1000 mB)");
                check(r.inputs().getFirst().items().getFirst().getCount() == 3, "item task count shown");
                check(r.outputs().size() == 2 && r.outputs().get(1).items().size() == 2, "outputs: item reward + reward table contents");
            }
            check(has(RecipeIndex.INSTANCE.questsUsing(new ItemStack(Items.IRON_INGOT)), RECIPE), "uses of iron ingot");
            check(has(RecipeIndex.INSTANCE.questsUsing(Fluids.WATER), RECIPE), "uses of water");
            check(has(RecipeIndex.INSTANCE.questsGiving(new ItemStack(Items.DIAMOND)), RECIPE), "recipes for diamond");
            check(RecipeIndex.INSTANCE.questsGiving(new ItemStack(Items.EMERALD)).isEmpty(), "hidden quest's reward has no recipe");
            List<CrateRecipe> crates = RecipeIndex.INSTANCE.crates();
            check(crates.size() == 1 && crates.getFirst().drops().size() == 2, "one loot crate with two drops: " + crates.size());
            if (!crates.isEmpty()) {
                CrateRecipe c = crates.getFirst();
                check(c.drops().getFirst().stack().is(Items.GOLDEN_APPLE) && c.chance(c.drops().getFirst()).equals("25%"),
                        "drops sorted rarest first with chance: " + c.chance(c.drops().getFirst()));
                check(c.dropChances().size() == 2, "mob drop lines for passive and monster only: " + c.dropChances().size());
                check(!RecipeIndex.INSTANCE.cratesGiving(new ItemStack(Items.APPLE)).isEmpty()
                        && RecipeIndex.INSTANCE.cratesOf(c.crateStack()).size() == 1, "crate lookups");
            }
            boolean jei = ModList.get().isLoaded("jei");
            String helper = FTBQuests.getRecipeModHelper().getHelperName();
            check(!jei || helper.equals("JEI"), "FTB Quests uses this module's helper: " + helper
                    + " (" + FTBQuests.getRecipeModHelper().getClass().getSimpleName() + ")");
            if (jei) {
                check(FTBQuests.getRecipeModHelper().getClass().getName().startsWith("com.questclusters"), "helper is ours, not XMod's");
            }
            return 1;
        });
        if (ModList.get().isLoaded("jei")) {
            steps.addAll(com.questclusters.xmod.jei.JeiDevTest.steps());
        } else if (ModList.get().isLoaded("roughlyenoughitems")) {
            steps.addAll(com.questclusters.xmod.rei.ReiDevTest.steps());
        }
        return steps;
    }
}
