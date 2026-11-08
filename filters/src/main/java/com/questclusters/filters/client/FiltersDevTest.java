package com.questclusters.filters.client;

import com.questclusters.client.DevSelfTest;
import com.questclusters.filters.Downgrade;
import com.questclusters.filters.client.screen.FuelScreen;
import com.questclusters.filters.client.screen.ItemListScreen;
import com.questclusters.filters.client.screen.TagListScreen;
import com.questclusters.filters.filter.FilterIds;
import com.questclusters.filters.filter.ItemListFilter;
import dev.ftb.mods.ftbfiltersystem.api.FTBFilterSystemAPI;
import dev.ftb.mods.ftbfiltersystem.api.NumericComparison;
import dev.ftb.mods.ftbfiltersystem.api.filter.SmartFilter;
import dev.ftb.mods.ftbfiltersystem.client.FTBFilterSystemClient;
import dev.ftb.mods.ftbfiltersystem.client.gui.FilterScreen;
import dev.ftb.mods.ftbfiltersystem.registry.FilterRegistry;
import dev.ftb.mods.ftbfiltersystem.registry.item.SmartFilterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
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

    private static SmartFilter parse(String s) {
        return FTBFilterSystemAPI.api().parseFilter(s, Minecraft.getInstance().level.registryAccess());
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
        SmartFilter child = ((SmartFilter.Compound) root).getChildren().getFirst();
        FTBFilterSystemClient.INSTANCE.openFilterConfigScreen(child, parent, false);
    }

    static void register() {
        DevSelfTest.addModuleSteps(List.<IntSupplier>of(
                () -> {
                    var reg = Minecraft.getInstance().level.registryAccess();
                    String list = "smart_filter_extras:items(minecraft:stone,minecraft:dirt)";
                    check(matches(list, new ItemStack(Items.STONE)) && matches(list, new ItemStack(Items.DIRT))
                            && !matches(list, new ItemStack(Items.COBBLESTONE)), "item list matches listed items only");
                    String healing = "smart_filter_extras:items(minecraft:potion[minecraft:potion_contents={potion:\"minecraft:healing\"}])";
                    ItemStack heal = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
                    ItemStack renamed = heal.copy();
                    renamed.set(DataComponents.CUSTOM_NAME, Component.literal("My (best) potion"));
                    check(matches(healing, heal) && matches(healing, renamed)
                            && !matches(healing, PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS)), "variant entry matches that potion only");
                    var missing = parse("smart_filter_extras:items(nomod:thing,minecraft:stone)");
                    check(missing.test(new ItemStack(Items.STONE)) && missing.asString(reg).contains("nomod:thing"), "missing ids kept, rest still works");
                    String tags = "smart_filter_extras:item_tag(#c:ingots,c:gems)";
                    check(matches(tags, new ItemStack(Items.IRON_INGOT)) && matches(tags, new ItemStack(Items.DIAMOND))
                            && !matches(tags, new ItemStack(Items.STONE)), "several item tags");
                    check(parse(tags).asString(reg).equals("smart_filter_extras:item_tag(c:ingots,c:gems)"), "tags saved without #: " + parse(tags).asString(reg));
                    String blocks = "smart_filter_extras:block_tag(minecraft:logs,minecraft:crops)";
                    check(matches(blocks, new ItemStack(Items.OAK_LOG)) && matches(blocks, new ItemStack(Items.WHEAT_SEEDS))
                            && !matches(blocks, new ItemStack(Items.STICK)), "block tags (seeds place crops)");
                    check(matches("smart_filter_extras:fuel(>=1600)", new ItemStack(Items.COAL)) && !matches("smart_filter_extras:fuel(>=1600)", new ItemStack(Items.STICK))
                            && matches("smart_filter_extras:fuel(=0)", new ItemStack(Items.STONE)), "fuel value");
                    var alias = parse("smartfilterextras:items(minecraft:stone)");
                    check(((SmartFilter.Compound) alias).getChildren().getFirst() instanceof ItemListFilter
                            && alias.asString(reg).startsWith("smart_filter_extras:items"), "2.x ids read and rewritten");
                    check(matches("ftbfiltersystemaddons:block_tag(minecraft:logs)", new ItemStack(Items.OAK_LOG)), "1.0.0 block_tag id read");
                    var multi = ((SmartFilter.Compound) parse("item(minecraft:stone,minecraft:dirt)")).getChildren().getFirst();
                    var single = ((SmartFilter.Compound) parse("item(minecraft:stone)")).getChildren().getFirst();
                    check(multi instanceof ItemListFilter && !(single instanceof ItemListFilter), "item(a,b) is a list, item(a) stays FTB's");
                    ItemStack named = new ItemStack(Items.STONE);
                    named.set(DataComponents.CUSTOM_NAME, Component.literal("a (b) \\ 100%"));
                    var entry = ItemListFilter.parseEntry(ItemListFilter.textOf(Items.STONE, named.getComponentsPatch(), reg), reg);
                    String saved = new ItemListFilter(null, List.of(entry)).asString(reg);
                    var back = ((SmartFilter.Compound) parse(saved)).getChildren().getFirst();
                    check(back instanceof ItemListFilter l && l.entries().getFirst().text().equals(entry.text()) && saved.contains("%28")
                            && saved.contains("%5C"), "brackets and backslashes in variants survive a save: " + saved);
                    var keys = FilterRegistry.getInstance().allFilterKeys();
                    long ours = FilterRegistry.getInstance().defaultFilterInstances().stream().filter(f -> f.getId().getNamespace().equals(FilterIds.NS)).count();
                    check(ours == 4 && keys.stream().noneMatch(k -> k.getNamespace().equals("smartfilterextras") || k.getNamespace().equals("ftbfiltersystemaddons")),
                            "each filter listed once in the Add menu");
                    var report = new Downgrade.Report();
                    String down = Downgrade.rewrite(parse("and(smart_filter_extras:items(minecraft:stone,minecraft:dirt)smart_filter_extras:item_tag(c:ingots)smart_filter_extras:fuel(>0))"), reg, report);
                    check(down.equals("and(or(item(minecraft:stone)item(minecraft:dirt))item_tag(c:ingots)smart_filter_extras:fuel(>0))")
                            && report.keptNeedsModule == 1 && parse(down).test(new ItemStack(Items.DIRT)) == false, "downgrade: " + down);
                    var vrep = new Downgrade.Report();
                    String vdown = Downgrade.rewrite(parse(healing), reg, vrep);
                    check(vdown.startsWith("and(item(minecraft:potion)component(fuzzy:") && parse(vdown).test(heal)
                            && !parse(vdown).test(PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS)), "downgrade keeps variant matches");
                    // the command on a held filter (server side)
                    var server = Minecraft.getInstance().getSingleplayerServer();
                    var uuid = Minecraft.getInstance().player.getUUID();
                    server.execute(() -> {
                        var sp = server.getPlayerList().getPlayer(uuid);
                        ItemStack f = new ItemStack(FTBFilterSystemAPI.api().filterItem());
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
                    check(s.shownForTest().stream().filter(c -> c.text().startsWith("minecraft:potion[")).count() > 5, "variants list potions separately");
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
                    check(root.asString(reg).equals("smart_filter_extras:items(minecraft:stone,minecraft:dirt)"), "Enter saves: " + root.asString(reg));
                    open("smart_filter_extras:block_tag(minecraft:planks)");
                    return 10;
                },
                () -> {
                    if (!(Minecraft.getInstance().screen instanceof TagListScreen<?> s)) {
                        check(false, "tag screen opens");
                        return 5;
                    }
                    s.reverseForTest(new ItemStack(Items.OAK_LOG));
                    check(s.shownForTest().contains(ResourceLocation.parse("minecraft:logs")) && !s.shownForTest().contains(ResourceLocation.parse("minecraft:planks")),
                            "reverse lookup shows the block's tags");
                    s.toggleForTest(ResourceLocation.parse("minecraft:logs"));
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
                    check(s.shownForTest().contains(ResourceLocation.parse("mymod:custom_ores")), "typed tag that is not loaded is offered");
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
                    check(root.asString(reg).equals("smart_filter_extras:block_tag(minecraft:planks,minecraft:logs)"), "tags saved: " + root.asString(reg));
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
                    check(root.asString(reg).equals("smart_filter_extras:fuel(>=1600)"), "fuel saved: " + root.asString(reg));
                    Minecraft.getInstance().setScreen(null);
                    return 5;
                }
        ));
    }

    private static volatile String downgraded;
}
