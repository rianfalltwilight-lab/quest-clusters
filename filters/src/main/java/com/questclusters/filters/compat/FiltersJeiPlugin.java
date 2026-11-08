package com.questclusters.filters.compat;

import com.questclusters.filters.client.screen.FuelScreen;
import com.questclusters.filters.client.screen.ItemListScreen;
import com.questclusters.filters.client.screen.TagListScreen;
import dev.ftb.mods.ftbfiltersystem.api.client.gui.AbstractFilterConfigScreen;
import dev.ftb.mods.ftbfiltersystem.client.gui.GhostDropReceiver;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.gui.handlers.IScreenHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** JEI (optional): its item list next to the filter screens, and dragging items into them. */
@JeiPlugin
public class FiltersJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("quest_clusters_filters", "jei");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration r) {
        if (net.neoforged.fml.ModList.get().isLoaded("ftbfiltersystem")) {
            Register.run(r);
            com.questclusters.filters.QuestClustersFilters.LOGGER.info("JEI: filter screens registered (item list and side panel, drag and drop)");
        }
    }

    /** Separate class: nothing from FTB Filter System is touched when it is not installed. */
    private static final class Register {
        static void run(IGuiHandlerRegistration r) {
        r.addGuiScreenHandler(ItemListScreen.class, new Props<>());
        r.addGhostIngredientHandler(ItemListScreen.class, new Ghost<>());
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<TagListScreen<?>> tags = (Class) TagListScreen.class;
        r.addGuiScreenHandler(tags, new Props<>());
        r.addGhostIngredientHandler(tags, new Ghost<>());
        r.addGuiScreenHandler(FuelScreen.class, new Props<>());
        }
    }

    static final class Props<T extends AbstractFilterConfigScreen<?>> implements IScreenHandler<T> {
        @Override
        public IGuiProperties apply(T s) {
            if (s.width == 0 || s.height == 0) return null;
            Rect2i b = s.getGuiBounds();
            return new IGuiProperties() {
                @Override
                public Class<? extends Screen> screenClass() {
                    return s.getClass();
                }

                @Override
                public int guiLeft() {
                    return b.getX();
                }

                @Override
                public int guiTop() {
                    return b.getY();
                }

                @Override
                public int guiXSize() {
                    return s.width - 2 * b.getX();
                }

                @Override
                public int guiYSize() {
                    return s.height - 2 * b.getY();
                }

                @Override
                public int screenWidth() {
                    return s.width;
                }

                @Override
                public int screenHeight() {
                    return s.height;
                }
            };
        }
    }

    static final class Ghost<T extends Screen & GhostDropReceiver> implements IGhostIngredientHandler<T> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(T gui, ITypedIngredient<I> ingredient, boolean doStart) {
            if (ingredient.getItemStack().isEmpty()) return List.of();
            return List.of(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return gui.getGhostDropRegion();
                }

                @Override
                public void accept(I i) {
                    if (i instanceof ItemStack stack) gui.receiveGhostDrop(stack);
                }
            });
        }

        @Override
        public void onComplete() {
        }
    }
}
