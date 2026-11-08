package com.questclusters.xmod.recipe;

import com.questclusters.xmod.QuestClustersXMod;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.integration.RecipeModHelper;

/** Picks the recipe viewer FTB Quests talks to: JEI if present, else REI (the same order as FTB XMod Compat). */
public final class RecipeViewers {
    private RecipeViewers() {
    }

    public static void install() {
        RecipeModHelper helper = null;
        if (QuestClustersXMod.loaded("jei")) {
            helper = new com.questclusters.xmod.jei.JeiHelper();
        } else if (QuestClustersXMod.loaded("roughlyenoughitems")) {
            helper = new com.questclusters.xmod.rei.ReiHelper();
        }
        if (helper != null) {
            FTBQuests.setRecipeModHelper(helper);
            QuestClustersXMod.LOGGER.info("FTB Quests recipe viewer: {}", helper.getHelperName());
        }
    }
}
