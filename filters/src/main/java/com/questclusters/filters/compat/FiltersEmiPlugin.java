package com.questclusters.filters.compat;

import com.questclusters.filters.client.screen.ItemListScreen;
import com.questclusters.filters.client.screen.TagListScreen;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.ftb.mods.ftbfiltersystem.client.gui.GhostDropReceiver;
import net.minecraft.client.gui.screens.Screen;

/**
 * EMI (optional): dragging items into the filter screens. EMI 1.1.24 on NeoForge only shows its sidebar on container
 * screens, which these are not, so this takes effect once EMI shows it on other screens too.
 */
@EmiEntrypoint
public class FiltersEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        if (net.neoforged.fml.ModList.get().isLoaded("ftbfiltersystem")) Register.run(registry);
    }

    /** Separate class: nothing from FTB Filter System is touched when it is not installed. */
    private static final class Register {
        static void run(EmiRegistry registry) {
        registry.addDragDropHandler(ItemListScreen.class, FiltersEmiPlugin::drop);
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<TagListScreen<?>> tags = (Class) TagListScreen.class;
        registry.addDragDropHandler(tags, FiltersEmiPlugin::drop);
        }
    }

    private static <T extends Screen & GhostDropReceiver> boolean drop(T screen, dev.emi.emi.api.stack.EmiIngredient ing, int x, int y) {
        if (ing.getEmiStacks().isEmpty() || !screen.getGhostDropRegion().contains(x, y)) return false;
        var stack = ing.getEmiStacks().getFirst().getItemStack();
        if (stack.isEmpty()) return false;
        screen.receiveGhostDrop(stack);
        return true;
    }
}
