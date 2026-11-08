package com.questclusters.xmod.jei;

import com.questclusters.xmod.recipe.CrateRecipe;
import com.questclusters.xmod.recipe.QuestRecipe;
import mezz.jei.api.recipe.RecipeType;

/** Uses FTB Quests' namespace, so JEI category ids stay the same as with FTB XMod Compat (hidden categories, etc.). */
final class JeiTypes {
    static final RecipeType<QuestRecipe> QUEST = RecipeType.create("ftbquests", "quest", QuestRecipe.class);
    static final RecipeType<CrateRecipe> LOOT_CRATE = RecipeType.create("ftbquests", "loot_crate", CrateRecipe.class);

    private JeiTypes() {
    }
}
