package com.questclusters.xmod.rei;

import com.questclusters.xmod.recipe.ViewerHelper;
import dev.architectury.fluid.FluidStack;
import dev.ftb.mods.ftblibrary.config.Tristate;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import me.shedaniel.rei.api.client.config.ConfigManager;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.favorites.FavoriteEntry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;

/** FTB Quests' recipe-viewer hook backed by REI: show recipes, favorites (REI's bookmarks), crate entries. */
public final class ReiHelper extends ViewerHelper {
    @Override
    public String getHelperName() {
        return "REI";
    }

    @Override
    public void showRecipes(ItemStack stack) {
        show(EntryStacks.of(stack));
    }

    @Override
    public void showRecipes(FluidStack fluid) {
        show(EntryStacks.of(fluid));
    }

    private static void show(EntryStack<?> entry) {
        ViewSearchBuilder.builder().addRecipesFor(entry).open();
    }

    @Override
    public Tristate toggleBookmark(ItemStack stack) {
        ConfigObject config = ConfigObject.getInstance();
        if (!config.isFavoritesEnabled()) {
            return Tristate.DEFAULT;
        }
        FavoriteEntry entry = FavoriteEntry.fromEntryStack(EntryStacks.of(stack.copyWithCount(1)));
        List<FavoriteEntry> favorites = config.getFavoriteEntries();
        boolean removed = favorites.removeIf(f -> f.isSame(entry));
        if (!removed) {
            favorites.add(entry);
        }
        ConfigManager.getInstance().saveConfig();
        return removed ? Tristate.FALSE : Tristate.TRUE;
    }

    @Override
    public boolean isBookmarkKey(Key key) {
        ConfigObject config = ConfigObject.getInstance();
        return config.isFavoritesEnabled() && config.getFavoriteKeyCode().matchesKey(key.keyCode, key.scanCode);
    }

    @Override
    public void updateItemsDynamic(Collection<ItemStack> toRemove, Collection<ItemStack> toAdd) {
        QuestsReiPlugin.syncCrates(toAdd);
    }
}
