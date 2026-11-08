package com.questclusters.xmod.jei;

import com.mojang.blaze3d.platform.InputConstants;
import com.questclusters.xmod.recipe.QuestRecipe;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.registry.ModItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Tasks in a 3x3 grid on the left, rewards on the right, the quest's title on top (click it to open the quest). */
final class QuestCategory implements IRecipeCategory<QuestRecipe> {
    /** FTB Quests ships this background for recipe viewers. */
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("ftbquests", "textures/gui/jei/quest.png");
    static final int WIDTH = 144;
    static final int HEIGHT = 74;
    private static final int TITLE_Y = 3;

    private final IDrawable background;
    private final IDrawable icon;

    QuestCategory(IGuiHelper gui) {
        background = gui.createDrawable(BACKGROUND, 0, 0, WIDTH, HEIGHT);
        icon = gui.createDrawableItemStack(new ItemStack(ModItems.BOOK.get()));
    }

    @Override
    public RecipeType<QuestRecipe> getRecipeType() {
        return JeiTypes.QUEST;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("ftbquests.quests");
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
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, QuestRecipe recipe, IFocusGroup focuses) {
        grid(builder, RecipeIngredientRole.INPUT, recipe.inputs(), 1);
        grid(builder, RecipeIngredientRole.OUTPUT, recipe.outputs(), 5 * 18 + 1);
    }

    /** A lone entry sits in the middle of its grid. */
    private static void grid(IRecipeLayoutBuilder builder, RecipeIngredientRole role, List<QuestRecipe.Slot> slots, int x0) {
        int offset = slots.size() == 1 ? 4 : 0;
        for (int i = 0; i < slots.size(); i++) {
            int cell = i + offset;
            QuestRecipe.Slot slot = slots.get(i);
            IRecipeSlotBuilder s = builder.addSlot(role, x0 + (cell % 3) * 18, 21 + (cell / 3) * 18);
            if (slot.isFluid()) {
                s.setFluidRenderer(Math.max(1, slot.fluidAmount()), false, 16, 16)
                        .addFluidStack(slot.fluid(), slot.fluidAmount(), slot.fluidComponents());
            } else {
                s.addItemStacks(slot.items());
            }
            if (slot.overflow() > 0) {
                s.addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("quest_clusters_xmod.recipe.more", slot.overflow()).withStyle(ChatFormatting.GRAY)));
            }
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, QuestRecipe recipe, IFocusGroup focuses) {
        builder.addInputHandler(new OpenQuest(recipe));
    }

    @Override
    public void draw(QuestRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics);
        Font font = Minecraft.getInstance().font;
        Component title = recipe.title().copy().withStyle(ChatFormatting.UNDERLINE);
        int w = Math.min(font.width(title), WIDTH - 4);
        int x = (WIDTH - w) / 2;
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= TITLE_Y && mouseY < TITLE_Y + font.lineHeight;
        graphics.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(title, WIDTH - 4)),
                x, TITLE_Y, hover ? 0xFFA87A5E : 0xFF3F2E23, false);
    }

    /** Clicking the title row opens the quest book on this quest. */
    private record OpenQuest(QuestRecipe recipe) implements IJeiInputHandler {
        @Override
        public ScreenRectangle getArea() {
            return new ScreenRectangle(0, 0, WIDTH, 18);
        }

        @Override
        public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
            if (input.getKey().getType() != InputConstants.Type.MOUSE) {
                return false;
            }
            if (!input.isSimulate()) {
                ClientQuestFile.openGui(recipe.quest(), true);
            }
            return true;
        }
    }
}
