package com.questclusters.xmod.recipe;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.integration.RecipeModHelper;
import dev.ftb.mods.ftbquests.integration.item_filtering.ItemMatchingSystem;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.loot.LootCrate;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import dev.ftb.mods.ftbquests.quest.loot.WeightedReward;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.RandomReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardAutoClaim;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Quests and loot crates of the client's quest book as recipes, rebuilt lazily after FTB Quests reports a change.
 * Lookups go through an item index first, so asking "what uses / gives this?" stays cheap with big books.
 * Client thread only.
 */
public final class RecipeIndex {
    public static final RecipeIndex INSTANCE = new RecipeIndex();

    private List<QuestRecipe> quests;
    private Map<Item, List<QuestRecipe>> questsByInput;
    private Map<Item, List<QuestRecipe>> questsByOutput;
    private List<CrateRecipe> crates;
    private Map<Item, List<CrateRecipe>> cratesByOutput;
    private int generation;

    private RecipeIndex() {
    }

    /** Bumped on every invalidation; viewers that copy recipes into their own registry compare it. */
    public int generation() {
        return generation;
    }

    public void invalidate(RecipeModHelper.Components component) {
        generation++;
        switch (component) {
            case QUESTS -> quests = null;
            case LOOT_CRATES -> crates = null;
        }
    }

    public void invalidateAll() {
        generation++;
        quests = null;
        crates = null;
    }

    // ---- quests ----

    public List<QuestRecipe> quests() {
        if (quests == null) {
            buildQuests();
        }
        return quests;
    }

    public List<QuestRecipe> questsUsing(ItemStack stack) {
        quests();
        return questsByInput.getOrDefault(stack.getItem(), List.of()).stream().filter(r -> r.usesItem(stack)).toList();
    }

    public List<QuestRecipe> questsUsing(Fluid fluid) {
        return quests().stream().filter(r -> r.usesFluid(fluid)).toList();
    }

    public List<QuestRecipe> questsGiving(ItemStack stack) {
        quests();
        return questsByOutput.getOrDefault(stack.getItem(), List.of()).stream().filter(r -> r.givesItem(stack)).toList();
    }

    private void buildQuests() {
        List<QuestRecipe> list = new ArrayList<>();
        if (ClientQuestFile.exists()) {
            ClientQuestFile file = ClientQuestFile.INSTANCE;
            file.forAllQuests(quest -> {
                QuestRecipe recipe = toRecipe(file, quest);
                if (recipe != null) {
                    list.add(recipe);
                }
            });
        }
        Map<Item, List<QuestRecipe>> in = new HashMap<>();
        Map<Item, List<QuestRecipe>> out = new HashMap<>();
        for (QuestRecipe recipe : list) {
            Set<Item> items = new HashSet<>();
            QuestRecipe.collectItems(recipe.inputs(), items);
            items.forEach(item -> in.computeIfAbsent(item, k -> new ArrayList<>()).add(recipe));
            items.clear();
            QuestRecipe.collectItems(recipe.outputs(), items);
            items.forEach(item -> out.computeIfAbsent(item, k -> new ArrayList<>()).add(recipe));
        }
        quests = List.copyOf(list);
        questsByInput = in;
        questsByOutput = out;
    }

    /**
     * Same visibility as FTB Quests' own lists: quests the team can start (no spoilers), searchable (not in an
     * always-invisible chapter), not opted out with "Disable in JEI", and with at least one visible reward.
     */
    private static QuestRecipe toRecipe(ClientQuestFile file, Quest quest) {
        if (!quest.showInRecipeMod() || quest.getRewards().isEmpty()
                || !file.selfTeamData.canStartTasks(quest) || !quest.isSearchable(file.selfTeamData)) {
            return null;
        }
        List<QuestRecipe.Slot> outputs = new ArrayList<>();
        for (Reward reward : quest.getRewards()) {
            if (reward.getAutoClaimType() != RewardAutoClaim.INVISIBLE) {
                QuestRecipe.Slot slot = rewardSlot(file, reward);
                if (slot != null) {
                    outputs.add(slot);
                }
            }
        }
        if (outputs.isEmpty()) {
            return null;
        }
        List<QuestRecipe.Slot> inputs = new ArrayList<>();
        for (Task task : quest.getTasks()) {
            inputs.add(taskSlot(file, task));
        }
        return new QuestRecipe(quest, fold(inputs), fold(outputs));
    }

    private static QuestRecipe.Slot taskSlot(ClientQuestFile file, Task task) {
        if (task instanceof FluidTask fluid) {
            return new QuestRecipe.Slot(List.of(), fluid.getFluid(), fluid.getMaxProgress(), fluid.getFluidDataComponentPatch(), s -> false, 0);
        }
        if (task instanceof ItemTask item && !item.getItemStack().isEmpty()) {
            int count = (int) Math.min(item.getMaxProgress(), Integer.MAX_VALUE);
            List<ItemStack> shown = item.getValidDisplayItems().stream().map(s -> s.copyWithCount(count)).toList();
            if (shown.isEmpty()) {
                shown = List.of(item.getItemStack().copyWithCount(count));
            }
            return QuestRecipe.Slot.ofItems(shown, item::test);
        }
        // any other task: its icon, expanded through FTB Quests' item filters when the icon is a filter item
        if (task.getIcon().getIngredient() instanceof ItemStack stack && !stack.isEmpty()) {
            List<ItemStack> all = ItemMatchingSystem.INSTANCE.getAllMatchingStacks(stack, file.holderLookup());
            List<ItemStack> shown = all.isEmpty() ? List.of(stack.copy()) : all;
            return QuestRecipe.Slot.ofItems(shown, s -> containsSame(shown, s));
        }
        ItemStack named = named(task.getIcon(), task.getTitle());
        return QuestRecipe.Slot.ofItems(List.of(named), s -> false);
    }

    private static QuestRecipe.Slot rewardSlot(ClientQuestFile file, Reward reward) {
        if (reward instanceof ItemReward item && !item.getItem().isEmpty()) {
            return QuestRecipe.Slot.ofItems(List.of(item.getItem().copyWithCount(Math.max(1, item.getCount()))), s -> false);
        }
        if (reward instanceof RandomReward random) {
            RewardTable table = random.getTable();
            if (table == null) {
                return null;
            }
            List<ItemStack> items = new ArrayList<>();
            if (table.shouldShowTooltip() || file.canEdit()) {
                for (WeightedReward wr : table.getWeightedRewards()) {
                    if (wr.getReward().getIcon().getIngredient() instanceof ItemStack s && !s.isEmpty()) {
                        items.add(s.copy());
                    }
                }
            }
            if (items.isEmpty()) {
                // hidden or non-item contents: one placeholder named after the reward
                items.add(named(Icon.empty(), reward.getTitle()));
            }
            return QuestRecipe.Slot.ofItems(items, s -> false);
        }
        Object ingredient = reward.getIcon().getIngredient();
        if (ingredient instanceof ItemStack s && !s.isEmpty()) {
            ItemStack shown = s.copy();
            shown.set(DataComponents.CUSTOM_NAME, reward.getTitle());
            return QuestRecipe.Slot.ofItems(List.of(shown), x -> false);
        }
        if (reward.getIcon() instanceof ItemIcon) {
            return QuestRecipe.Slot.ofItems(List.of(named(reward.getIcon(), reward.getTitle())), x -> false);
        }
        return null; // XP, commands and the like without an item icon have nothing to show in an item slot
    }

    /** Up to {@link QuestRecipe#GRID} slots; the rest are folded into the last slot, which then cycles. */
    private static List<QuestRecipe.Slot> fold(List<QuestRecipe.Slot> slots) {
        if (slots.size() <= QuestRecipe.GRID) {
            return List.copyOf(slots);
        }
        List<QuestRecipe.Slot> out = new ArrayList<>(slots.subList(0, QuestRecipe.GRID - 1));
        List<QuestRecipe.Slot> rest = slots.subList(QuestRecipe.GRID - 1, slots.size());
        List<ItemStack> items = new ArrayList<>();
        List<Predicate<ItemStack>> tests = new ArrayList<>();
        for (QuestRecipe.Slot slot : rest) {
            if (slot.isFluid()) {
                items.add(slot.fluid().getBucket() == Items.AIR ? new ItemStack(Items.BUCKET) : new ItemStack(slot.fluid().getBucket()));
            } else {
                items.addAll(slot.items());
                tests.add(slot.accepts());
            }
        }
        out.add(new QuestRecipe.Slot(List.copyOf(items), null, 0, net.minecraft.core.component.DataComponentPatch.EMPTY, s -> tests.stream().anyMatch(t -> t.test(s)), rest.size() - 1));
        return out;
    }

    private static boolean containsSame(List<ItemStack> list, ItemStack stack) {
        for (ItemStack s : list) {
            if (ItemStack.isSameItemSameComponents(s, stack)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack named(Icon icon, Component title) {
        ItemStack stack = icon instanceof ItemIcon itemIcon ? itemIcon.getStack().copyWithCount(1) : new ItemStack(Items.PAINTING);
        stack.set(DataComponents.CUSTOM_NAME, title);
        return stack;
    }

    // ---- loot crates ----

    public List<CrateRecipe> crates() {
        if (crates == null) {
            buildCrates();
        }
        return crates;
    }

    public List<CrateRecipe> cratesOf(ItemStack crateStack) {
        return crates().stream().filter(c -> c.isCrate(crateStack)).toList();
    }

    public List<CrateRecipe> cratesGiving(ItemStack stack) {
        crates();
        return cratesByOutput.getOrDefault(stack.getItem(), List.of());
    }

    /** The crate items of the current book, for viewers that list them as ingredients. */
    public List<ItemStack> crateStacks() {
        return crates().stream().map(CrateRecipe::crateStack).toList();
    }

    private void buildCrates() {
        List<CrateRecipe> list = new ArrayList<>();
        if (ClientQuestFile.exists()) {
            for (RewardTable table : ClientQuestFile.INSTANCE.getRewardTables()) {
                LootCrate crate = table.getLootCrate();
                if (crate != null) {
                    list.add(toRecipe(crate));
                }
            }
        }
        Map<Item, List<CrateRecipe>> out = new HashMap<>();
        for (CrateRecipe recipe : list) {
            Set<Item> items = new LinkedHashSet<>();
            recipe.drops().forEach(d -> items.add(d.stack().getItem()));
            items.forEach(item -> out.computeIfAbsent(item, k -> new ArrayList<>()).add(recipe));
        }
        crates = List.copyOf(list);
        cratesByOutput = out;
    }

    private static CrateRecipe toRecipe(LootCrate crate) {
        RewardTable table = crate.getTable();
        List<CrateRecipe.Drop> drops = table.getWeightedRewards().stream()
                .sorted(Comparator.comparingDouble(WeightedReward::getWeight))
                .map(wr -> new CrateRecipe.Drop(dropStack(wr.getReward()), wr.getWeight()))
                .toList();
        return new CrateRecipe(crate, crate.createStack(), drops, table.getTotalWeight(true));
    }

    private static ItemStack dropStack(Reward reward) {
        if (reward instanceof ItemReward item && !item.getItem().isEmpty()) {
            return item.getItem().copyWithCount(Math.max(1, item.getCount()));
        }
        if (reward.getIcon().getIngredient() instanceof ItemStack s && !s.isEmpty()) {
            return s.copy();
        }
        return named(reward.getIcon(), reward.getTitle());
    }
}
