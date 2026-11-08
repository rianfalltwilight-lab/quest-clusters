package com.questclusters.xmod.rei;

import com.questclusters.xmod.recipe.CrateRecipe;
import com.questclusters.xmod.recipe.RecipeIndex;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.registry.ModItems;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** The two REI categories; same layout as the JEI ones. */
final class ReiCategories {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("ftbquests", "textures/gui/jei/quest.png");

    private ReiCategories() {
    }

    static final class Quest implements DisplayCategory<ReiDisplays.QuestDisplay> {
        private final Renderer icon = EntryStacks.of(ModItems.BOOK.get());

        @Override
        public CategoryIdentifier<? extends ReiDisplays.QuestDisplay> getCategoryIdentifier() {
            return ReiDisplays.QUEST;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("ftbquests.quests");
        }

        @Override
        public Renderer getIcon() {
            return icon;
        }

        @Override
        public int getDisplayHeight() {
            return 74 + 8;
        }

        @Override
        public int getDisplayWidth(ReiDisplays.QuestDisplay display) {
            return 144 + 8;
        }

        @Override
        public List<Widget> setupDisplay(ReiDisplays.QuestDisplay display, Rectangle bounds) {
            int x = bounds.x + 4;
            int y = bounds.y + 4;
            List<Widget> widgets = new ArrayList<>();
            widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(Widgets.createTexturedWidget(BACKGROUND, x, y, 0, 0, 144, 74));
            widgets.add(Widgets.createClickableLabel(new Point(x + 72, y + 3),
                            display.recipe().title().copy().withStyle(ChatFormatting.UNDERLINE),
                            label -> ClientQuestFile.openGui(display.recipe().quest(), true))
                    .centered().noShadow().color(0xFF3F2E23, 0xFFBBBBBB)
                    .tooltip(Component.translatable("quest_clusters_xmod.recipe.open")));
            grid(widgets, display.inputs(), x + 1, y + 21, true);
            grid(widgets, display.outputs(), x + 5 * 18 + 1, y + 21, false);
            return widgets;
        }

        private static void grid(List<Widget> widgets, List<EntryIngredient> slots, int x0, int y0, boolean input) {
            int offset = slots.size() == 1 ? 4 : 0;
            for (int i = 0; i < slots.size(); i++) {
                int cell = i + offset;
                var slot = Widgets.createSlot(new Point(x0 + (cell % 3) * 18, y0 + (cell / 3) * 18)).entries(slots.get(i)).disableBackground();
                widgets.add(input ? slot.markInput() : slot.markOutput());
            }
        }
    }

    static final class Crate implements DisplayCategory<ReiDisplays.CrateDisplay> {
        /** Cycles through the book's crates once a second. */
        private final Renderer icon = new Renderer() {
            @Override
            public void render(GuiGraphics graphics, Rectangle bounds, int mouseX, int mouseY, float delta) {
                List<ItemStack> crates = RecipeIndex.INSTANCE.crateStacks();
                ItemStack stack = crates.isEmpty() ? new ItemStack(ModItems.LOOTCRATE.get())
                        : crates.get((int) (System.currentTimeMillis() / 1000L % crates.size()));
                graphics.renderItem(stack, bounds.getCenterX() - 8, bounds.getCenterY() - 8);
            }
        };

        @Override
        public CategoryIdentifier<? extends ReiDisplays.CrateDisplay> getCategoryIdentifier() {
            return ReiDisplays.LOOT_CRATE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.ftbquests.lootcrates");
        }

        @Override
        public Renderer getIcon() {
            return icon;
        }

        @Override
        public int getDisplayHeight() {
            return CrateRecipe.ROWS * 18 + 36 + 8;
        }

        @Override
        public int getDisplayWidth(ReiDisplays.CrateDisplay display) {
            return CrateRecipe.COLUMNS * 18 + 8;
        }

        @Override
        public int getMaximumDisplaysPerPage() {
            return 1;
        }

        @Override
        public List<Widget> setupDisplay(ReiDisplays.CrateDisplay display, Rectangle bounds) {
            int x = bounds.x + 4;
            int y = bounds.y + 4;
            int width = CrateRecipe.COLUMNS * 18;
            List<Widget> widgets = new ArrayList<>();
            widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(Widgets.createLabel(new Point(x + 1, y), display.recipe().title().copy().withStyle(ChatFormatting.UNDERLINE))
                    .leftAligned().color(0xFFFFFF00));
            widgets.add(Widgets.createSlot(new Point(x + 1, y + 9)).entries(display.getInputEntries().getFirst()).markInput());
            int line = 0;
            for (Component chance : display.recipe().dropChances()) {
                widgets.add(Widgets.createLabel(new Point(x + width - 1, y + 9 + line++ * 9), chance).rightAligned().noShadow().color(0xFF404040, 0xFFBBBBBB));
            }
            List<EntryIngredient> outputs = display.getOutputEntries();
            for (int i = 0; i < outputs.size(); i++) {
                widgets.add(Widgets.createSlot(new Point(x + (i % CrateRecipe.COLUMNS) * 18 + 1, y + 37 + (i / CrateRecipe.COLUMNS) * 18))
                        .entries(outputs.get(i)).markOutput());
            }
            return widgets;
        }
    }
}
