package com.questclusters.xmod.jei;

import com.questclusters.xmod.recipe.CrateRecipe;
import com.questclusters.xmod.recipe.RecipeIndex;
import dev.ftb.mods.ftbquests.registry.ModItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The crate in the top-left corner, its name and mob drop chances beside it, its contents in an 8x6 grid. */
final class CrateCategory implements IRecipeCategory<CrateRecipe> {
    static final int WIDTH = CrateRecipe.COLUMNS * 18;
    static final int HEIGHT = CrateRecipe.ROWS * 18 + 36;

    /** Cycles through the book's crates once a second (the plain crate item when there are none). */
    private static final IDrawable ICON = new IDrawable() {
        @Override
        public int getWidth() {
            return 16;
        }

        @Override
        public int getHeight() {
            return 16;
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y) {
            List<ItemStack> crates = RecipeIndex.INSTANCE.crateStacks();
            ItemStack stack = crates.isEmpty() ? new ItemStack(ModItems.LOOTCRATE.get())
                    : crates.get((int) (System.currentTimeMillis() / 1000L % crates.size()));
            graphics.renderItem(stack, x, y);
        }
    };

    @Override
    public RecipeType<CrateRecipe> getRecipeType() {
        return JeiTypes.LOOT_CRATE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.ftbquests.lootcrates");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return ICON;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrateRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 9).setStandardSlotBackground().addIngredient(VanillaTypes.ITEM_STACK, recipe.crateStack());
        List<List<CrateRecipe.Drop>> slots = recipe.slots();
        for (int i = 0; i < slots.size(); i++) {
            List<CrateRecipe.Drop> drops = slots.get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, (i % CrateRecipe.COLUMNS) * 18 + 1, (i / CrateRecipe.COLUMNS) * 18 + 37)
                    .setStandardSlotBackground()
                    .addItemStacks(drops.stream().map(CrateRecipe.Drop::stack).toList())
                    .addRichTooltipCallback((view, tooltip) -> view.getDisplayedItemStack().ifPresent(shown -> {
                        for (CrateRecipe.Drop drop : drops) {
                            if (ItemStack.isSameItemSameComponents(drop.stack(), shown)) {
                                tooltip.add(Component.translatable("jei.ftbquests.lootcrates.chance",
                                        Component.literal(recipe.chance(drop)).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY));
                                return;
                            }
                        }
                    }));
        }
    }

    @Override
    public void draw(CrateRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, recipe.title().copy().withStyle(ChatFormatting.UNDERLINE), 1, 0, 0xFFFFFF00);
        // mob drop chances, right-aligned beside the crate slot (three lines fit above the grid)
        int y = 9;
        for (Component line : recipe.dropChances()) {
            graphics.drawString(font, line, WIDTH - font.width(line) - 1, y, 0xFF404040, false);
            y += font.lineHeight;
        }
    }
}
