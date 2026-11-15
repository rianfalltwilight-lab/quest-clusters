package com.questclusters.filters.client;

import com.questclusters.platform.Rl;
import com.questclusters.client.DevSelfTest;
import com.questclusters.filters.Downgrade;
import com.questclusters.filters.client.screen.FuelScreen;
import com.questclusters.filters.client.screen.ItemListScreen;
import com.questclusters.filters.client.screen.TagListScreen;
import com.questclusters.filters.filter.FilterIds;
import com.questclusters.filters.filter.FfsCompat;
import com.questclusters.filters.filter.ItemListFilter;
import com.questclusters.filters.filter.ItemVariant;
import dev.ftb.mods.ftbfiltersystem.api.FTBFilterSystemAPI;
import dev.ftb.mods.ftbfiltersystem.api.NumericComparison;
import dev.ftb.mods.ftbfiltersystem.api.filter.SmartFilter;
import dev.ftb.mods.ftbfiltersystem.client.FTBFilterSystemClient;
import dev.ftb.mods.ftbfiltersystem.client.gui.FilterScreen;
import dev.ftb.mods.ftbfiltersystem.registry.FilterRegistry;
import dev.ftb.mods.ftbfiltersystem.registry.item.SmartFilterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
//? if neoforge {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if neoforge {
import net.minecraft.world.item.alchemy.PotionContents;
//?}
import net.minecraft.world.item.alchemy.Potions;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.IntSupplier;

/** Filter checks for the dev self-test (with -PdevModules=filters). */
final class FiltersDevTest {
    private static SmartFilter root;
    private static FilterScreen parent;

    private FiltersDevTest() {
    }

    //? if neoforge {
    private static FilterRegistry filterRegistry() {
        return FilterRegistry.getInstance();
    }

    private static ItemStack filterStack() {
        return new ItemStack(FTBFilterSystemAPI.api().filterItem());
    }
    //?} else {
    /*private static FilterRegistry filterRegistry() {
        return FilterRegistry.INSTANCE;
    }

    private static ItemStack filterStack() {
        return new ItemStack(dev.ftb.mods.ftbfiltersystem.registry.ModItems.SMART_FILTER.get());
    }
    *///?}

    private static SmartFilter parse(String s) {
        //? if neoforge {
        return FTBFilterSystemAPI.api().parseFilter(s, Minecraft.getInstance().level.registryAccess());
        //?} else {
        /*return FTBFilterSystemAPI.api().parseFilter(s);
        *///?}
    }

    private static boolean matches(String filter, ItemStack stack) {
        return parse(filter).test(stack);
    }

    private static void check(boolean ok, String what) {
        DevSelfTest.moduleCheck(ok, "filters: " + what);
    }

    private static void open(String filter) {
        var mc = Minecraft.getInstance();
        root = parse(filter);
        parent = new FilterScreen(Component.literal("test"), root, InteractionHand.MAIN_HAND);
        mc.setScreen(parent);
        SmartFilter child = ((SmartFilter.Compound) root).getChildren().get(0);
        FTBFilterSystemClient.INSTANCE.openFilterConfigScreen(child, parent, false);
    }

    /** Common tags: c: on NeoForge 1.21.1, forge: on Forge 1.20.1. */
    private static final String TAGS = DevSelfTest.LEGACY ? "forge" : "c";

    //? if neoforge {
    private static final String HEALING_DATA = "[minecraft:potion_contents={potion:\"minecraft:healing\"}]";

    private static ItemStack healing() {
        return PotionContents.createItemStack(Items.POTION, Potions.HEALING);
    }

    private static ItemStack swiftness() {
        return PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
    }

    private static ItemStack rename(ItemStack stack, Component name) {
        stack.set(DataComponents.CUSTOM_NAME, name);
        return stack;
    }
    //?} else {
    /*private static final String HEALING_DATA = "{Potion:\"minecraft:healing\"}";

    private static ItemStack healing() {
        return net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING);
    }

    private static ItemStack swiftness() {
        return net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.SWIFTNESS);
    }

    private static ItemStack rename(ItemStack stack, Component name) {
        return stack.setHoverName(name);
    }
    *///?}

    static void register() {
        DevSelfTest.addModuleSteps(List.<IntSupplier>of(
                () -> {
                    var reg = Minecraft.getInstance().level.registryAccess();
                    String list = "smart_filter_extras:items(minecraft:stone,minecraft:dirt)";
                    check(matches(list, new ItemStack(Items.STONE)) && matches(list, new ItemStack(Items.DIRT))
                            && !matches(list, new ItemStack(Items.COBBLESTONE)), "item list matches listed items only");
                    String healing = "smart_filter_extras:items(minecraft:potion" + HEALING_DATA + ")";
                    ItemStack heal = healing();
                    ItemStack renamed = rename(heal.copy(), Component.literal("My (best) potion"));
                    check(matches(healing, heal) && matches(healing, renamed)
                            && !matches(healing, swiftness()), "variant entry matches that potion only");
                    var missing = parse("smart_filter_extras:items(nomod:thing,minecraft:stone)");
                    check(missing.test(new ItemStack(Items.STONE)) && FfsCompat.asString(missing, reg).contains("nomod:thing"), "missing ids kept, rest still works");
                    String tags = "smart_filter_extras:item_tag(#" + TAGS + ":ingots," + TAGS + ":gems)";
                    check(matches(tags, new ItemStack(Items.IRON_INGOT)) && matches(tags, new ItemStack(Items.DIAMOND))
                            && !matches(tags, new ItemStack(Items.STONE)), "several item tags");
                    check(FfsCompat.asString(parse(tags), reg).equals("smart_filter_extras:item_tag(" + TAGS + ":ingots," + TAGS + ":gems)"), "tags saved without #: " + FfsCompat.asString(parse(tags), reg));
                    String blocks = "smart_filter_extras:block_tag(minecraft:logs,minecraft:crops)";
                    check(matches(blocks, new ItemStack(Items.OAK_LOG)) && matches(blocks, new ItemStack(Items.WHEAT_SEEDS))
                            && !matches(blocks, new ItemStack(Items.STICK)), "block tags (seeds place crops)");
                    check(matches("smart_filter_extras:fuel(>=1600)", new ItemStack(Items.COAL)) && !matches("smart_filter_extras:fuel(>=1600)", new ItemStack(Items.STICK))
                            && matches("smart_filter_extras:fuel(=0)", new ItemStack(Items.STONE)), "fuel value (coal "
                            + com.questclusters.filters.filter.FuelFilter.burnTime(new ItemStack(Items.COAL)) + ", " + FfsCompat.asString(parse("smart_filter_extras:fuel(>=1600)"), reg) + ")");
                    var alias = parse("smartfilterextras:items(minecraft:stone)");
                    check(((SmartFilter.Compound) alias).getChildren().get(0) instanceof ItemListFilter
                            && FfsCompat.asString(alias, reg).startsWith("smart_filter_extras:items"), "2.x ids read and rewritten");
                    check(matches("ftbfiltersystemaddons:block_tag(minecraft:logs)", new ItemStack(Items.OAK_LOG)), "1.0.0 block_tag id read");
                    var multi = ((SmartFilter.Compound) parse("item(minecraft:stone,minecraft:dirt)")).getChildren().get(0);
                    var single = ((SmartFilter.Compound) parse("item(minecraft:stone)")).getChildren().get(0);
                    check(multi instanceof ItemListFilter && !(single instanceof ItemListFilter), "item(a,b) is a list, item(a) stays FTB's");
                    ItemStack named = rename(new ItemStack(Items.STONE), Component.literal("a (b) \\ 100%"));
                    var entry = ItemListFilter.parseEntry(ItemListFilter.textOf(Items.STONE, ItemVariant.of(named), reg), reg);
                    String saved = FfsCompat.asString(new ItemListFilter(null, List.of(entry)), reg);
                    var back = ((SmartFilter.Compound) parse(saved)).getChildren().get(0);
                    check(back instanceof ItemListFilter l && l.entries().get(0).text().equals(entry.text()) && saved.contains("%28")
                            && saved.contains("%5C"), "brackets and backslashes in variants survive a save: " + saved);
                    var keys = filterRegistry().allFilterKeys();
                    long ours = filterRegistry().defaultFilterInstances().stream().filter(f -> f.getId().getNamespace().equals(FilterIds.NS)).count();
                    check(ours == 4 && keys.stream().noneMatch(k -> k.getNamespace().equals("smartfilterextras") || k.getNamespace().equals("ftbfiltersystemaddons")),
                            "each filter listed once in the Add menu");
                    var report = new Downgrade.Report();
                    String down = Downgrade.rewrite(parse("and(smart_filter_extras:items(minecraft:stone,minecraft:dirt)smart_filter_extras:item_tag(c:ingots)smart_filter_extras:fuel(>0))"), reg, report);
                    check(down.equals("and(or(item(minecraft:stone)item(minecraft:dirt))item_tag(c:ingots)smart_filter_extras:fuel(>0))")
                            && report.keptNeedsModule == 1 && parse(down).test(new ItemStack(Items.DIRT)) == false, "downgrade: " + down);
                    var vrep = new Downgrade.Report();
                    String vdown = Downgrade.rewrite(parse(healing), reg, vrep);
                    check(vdown.startsWith("and(item(minecraft:potion)" + ItemVariant.NATIVE_DATA_FILTER + "(fuzzy:") && parse(vdown).test(heal)
                            && !parse(vdown).test(swiftness()), "downgrade keeps variant matches");
                    // the command on a held filter (server side)
                    var server = Minecraft.getInstance().getSingleplayerServer();
                    var uuid = Minecraft.getInstance().player.getUUID();
                    server.execute(() -> {
                        var sp = server.getPlayerList().getPlayer(uuid);
                        ItemStack f = filterStack();
                        SmartFilterItem.setFilter(f, "smart_filter_extras:items(minecraft:stone,nomod:gone)");
                        sp.setItemInHand(InteractionHand.MAIN_HAND, f);
                        server.getCommands().performPrefixedCommand(sp.createCommandSourceStack(), "qcfilters downgrade");
                        downgraded = SmartFilterItem.getFilterString(sp.getMainHandItem());
                        sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    });
                    return 10;
                },
                () -> {
                    check("item(minecraft:stone)".equals(downgraded), "downgrade command (missing id dropped): " + downgraded);
                    open("smart_filter_extras:items(minecraft:stone)");
                    return 10;
                },
                () -> {
                    if (!(Minecraft.getInstance().screen instanceof ItemListScreen s)) {
                        check(false, "item list screen opens (" + Minecraft.getInstance().screen + ")");
                        return 5;
                    }
                    s.setVariantsForTest(false);
                    s.searchForTest("dirt");
                    return 5;
                },
                () -> {
                    var s = (ItemListScreen) Minecraft.getInstance().screen;
                    check(s.shownForTest().stream().anyMatch(c -> c.text().equals("minecraft:dirt")), "search finds dirt (" + s.shownForTest().size() + " shown)");
                    s.searchForTest("@minecraft stone");
                    check(s.addForTest("minecraft:dirt") && s.selectedForTest().size() == 2, "item added");
                    s.setVariantsForTest(true);
                    s.searchForTest("potion");
                    return 5;
                },
                () -> {
                    var s = (ItemListScreen) Minecraft.getInstance().screen;
                    check(s.shownForTest().stream().filter(c -> c.text().startsWith("minecraft:potion" + ItemVariant.OPEN)).count() > 5, "variants list potions separately");
                    DevSelfTest.moduleShot("qc_40_filters_items");
                    s.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
                    return 5;
                },
                () -> {
                    check(Minecraft.getInstance().screen instanceof ConfirmScreen, "Esc with changes asks first");
                    Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
                    return 5;
                },
                () -> {
                    var s = (ItemListScreen) Minecraft.getInstance().screen;
                    check(s.selectedForTest().size() == 2, "Esc on the question keeps editing");
                    s.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
                    return 5;
                },
                () -> {
                    var reg = Minecraft.getInstance().level.registryAccess();
                    check(FfsCompat.asString(root, reg).equals("smart_filter_extras:items(minecraft:stone,minecraft:dirt)"), "Enter saves: " + FfsCompat.asString(root, reg));
                    open("smart_filter_extras:block_tag(minecraft:planks)");
                    return 10;
                },
                () -> {
                    if (!(Minecraft.getInstance().screen instanceof TagListScreen<?> s)) {
                        check(false, "tag screen opens");
                        return 5;
                    }
                    s.reverseForTest(new ItemStack(Items.OAK_LOG));
                    check(s.shownForTest().contains(Rl.parse("minecraft:logs")) && !s.shownForTest().contains(Rl.parse("minecraft:planks")),
                            "reverse lookup shows the block's tags");
                    s.toggleForTest(Rl.parse("minecraft:logs"));
                    s.searchForTest("#mymod:custom_ores");
                    return 5;
                },
                () -> {
                    var s = (TagListScreen<?>) Minecraft.getInstance().screen;
                    s.reverseForTest(ItemStack.EMPTY);
                    return 3;
                },
                () -> {
                    var s = (TagListScreen<?>) Minecraft.getInstance().screen;
                    check(s.shownForTest().contains(Rl.parse("mymod:custom_ores")), "typed tag that is not loaded is offered");
                    s.searchForTest("logs");
                    return 15;
                },
                () -> {
                    DevSelfTest.moduleShot("qc_41_filters_tags");
                    var s = (TagListScreen<?>) Minecraft.getInstance().screen;
                    check(s.checkedForTest().size() == 2, "two block tags checked");
                    s.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
                    return 5;
                },
                () -> {
                    var reg = Minecraft.getInstance().level.registryAccess();
                    check(FfsCompat.asString(root, reg).equals("smart_filter_extras:block_tag(minecraft:planks,minecraft:logs)"), "tags saved: " + FfsCompat.asString(root, reg));
                    open("smart_filter_extras:fuel(>0)");
                    return 10;
                },
                () -> {
                    if (!(Minecraft.getInstance().screen instanceof FuelScreen s)) {
                        check(false, "fuel screen opens");
                        return 5;
                    }
                    s.setForTest(NumericComparison.ComparisonOp.GE, 1600);
                    check(s.matchingForTest().stream().anyMatch(i -> i.is(Items.COAL)) && s.matchingForTest().stream().noneMatch(i -> i.is(Items.STICK)),
                            "fuel preview lists matching fuels");
                    return 5;
                },
                () -> {
                    DevSelfTest.moduleShot("qc_42_filters_fuel");
                    Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
                    return 5;
                },
                () -> {
                    var reg = Minecraft.getInstance().level.registryAccess();
                    check(FfsCompat.asString(root, reg).equals("smart_filter_extras:fuel(>=1600)"), "fuel saved: " + FfsCompat.asString(root, reg));
                    Minecraft.getInstance().setScreen(null);
                    return 5;
                }
        ));
    }

    private static volatile String downgraded;
}
