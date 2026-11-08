package com.questclusters.xmod.recipe;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.loot.EntityWeight;
import dev.ftb.mods.ftbquests.quest.loot.LootCrate;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import dev.ftb.mods.ftbquests.quest.loot.WeightedReward;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * A loot crate as a recipe: the crate on top, its weighted contents below (least likely first, as in FTB Quests'
 * own crate screen). More contents than {@link #SLOTS} cycle through the slots.
 */
public record CrateRecipe(LootCrate crate, ItemStack crateStack, List<Drop> drops, float totalWeight) {
    public static final int COLUMNS = 8;
    public static final int ROWS = 6;
    public static final int SLOTS = COLUMNS * ROWS;

    public record Drop(ItemStack stack, float weight) {
    }

    public Component title() {
        return Component.translatable(crate.getItemName());
    }

    public String chance(Drop drop) {
        return WeightedReward.chanceString(drop.weight, totalWeight);
    }

    /** The drops shown in each slot: one each, or cycling when there are more drops than slots. */
    public List<List<Drop>> slots() {
        int n = Math.min(SLOTS, drops.size());
        List<List<Drop>> slots = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            slots.add(new ArrayList<>());
        }
        for (int i = 0; i < drops.size(); i++) {
            slots.get(i % SLOTS).add(drops.get(i));
        }
        return slots;
    }

    /** Lines for how often mobs drop this crate; types it never drops from are left out. */
    public List<Component> dropChances() {
        List<Component> lines = new ArrayList<>(3);
        if (!ClientQuestFile.exists()) {
            return lines;
        }
        line(lines, "passive", w -> w.passive);
        line(lines, "monster", w -> w.monster);
        line(lines, "boss", w -> w.boss);
        return lines;
    }

    private void line(List<Component> lines, String type, ToIntFunction<EntityWeight> weight) {
        int own = weight.applyAsInt(crate.getDrops());
        if (own <= 0) {
            return;
        }
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        int total = weight.applyAsInt(file.getLootCrateNoDrop());
        for (RewardTable table : file.getRewardTables()) {
            if (table.getLootCrate() != null) {
                total += weight.applyAsInt(table.getLootCrate().getDrops());
            }
        }
        lines.add(Component.translatable("ftbquests.loot.entitytype." + type)
                .append(": ")
                .append(Component.literal(WeightedReward.chanceString(own, total)).withStyle(ChatFormatting.GOLD)));
    }

    public boolean givesItem(ItemStack stack) {
        for (Drop drop : drops) {
            if (drop.stack.is(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    public boolean isCrate(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(crateStack, stack);
    }
}
