package com.questclusters.xmod.recipe;

import dev.ftb.mods.ftbquests.integration.RecipeModHelper;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;

/**
 * The part of FTB Quests' recipe-viewer hook that is the same for every viewer: changes to the book drop the
 * matching half of {@link RecipeIndex}, then the viewer gets a chance to refresh what it shows.
 */
public abstract class ViewerHelper implements RecipeModHelper {
    @Override
    public final void refreshAll(Components component) {
        RecipeIndex.INSTANCE.invalidate(component);
        onChanged(component);
    }

    @Override
    public final void refreshRecipes(QuestObjectBase object) {
        if (object != null) {
            object.componentsToRefresh().forEach(this::refreshAll);
        }
    }

    @Override
    public boolean isRecipeModAvailable() {
        return true;
    }

    /** After {@link RecipeIndex} dropped {@code component}. */
    protected void onChanged(Components component) {
    }
}
