package com.questclusters.xmod.client;

import com.questclusters.client.DevSelfTest;
import com.questclusters.xmod.QuestClustersXMod;
import dev.ftb.mods.ftbquests.integration.item_filtering.ItemMatchingSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

import java.util.List;
import java.util.function.IntSupplier;

/** XMod-part checks for the dev self-test (FFS adapter; KubeJS with -PwithKubejs; take-over with -PwithXmod). */
final class XModDevTest {
    private static volatile String binding, last;
    private static volatile int count = -1;
    private static volatile boolean tag, data;

    private XModDevTest() {
    }

    /** KubeJS keeps its own per-player data ({@code player.persistentData} in scripts). */
    private static net.minecraft.nbt.CompoundTag kjs(net.minecraft.world.entity.player.Player p) {
        try {
            return (net.minecraft.nbt.CompoundTag) p.getClass().getMethod("kjs$getPersistentData").invoke(p);
        } catch (ReflectiveOperationException e) {
            return new net.minecraft.nbt.CompoundTag();
        }
    }

    private static void check(boolean ok, String what) {
        DevSelfTest.moduleCheck(ok, "xmod: " + what);
    }

    static void register() {
        DevSelfTest.addModuleSteps(RecipeDevTest.steps());
        DevSelfTest.addModuleSteps(List.<IntSupplier>of(
                () -> {
                    boolean xmod = ModList.get().isLoaded("ftbxmodcompat");
                    if (ModList.get().isLoaded("ftbfiltersystem")) {
                        long ffs = ItemMatchingSystem.INSTANCE.adapters().stream().filter(a -> a.getName().equals("FTB Filter System")).count();
                        check(ffs == 1, "one FTB Filter System adapter for item tasks" + (xmod ? " (XMod's is switched off)" : "") + ": " + ffs);
                        var reg = Minecraft.getInstance().level.registryAccess();
                        ItemStack filter = new ItemStack(dev.ftb.mods.ftbfiltersystem.api.FTBFilterSystemAPI.api().filterItem());
                        dev.ftb.mods.ftbfiltersystem.registry.item.SmartFilterItem.setFilter(filter, "smart_filter_extras:items(minecraft:stone,minecraft:dirt)");
                        var none = ItemMatchingSystem.ComponentMatchType.NONE;
                        check(ItemMatchingSystem.INSTANCE.doesItemMatch(filter, new ItemStack(Items.DIRT), none, reg)
                                && !ItemMatchingSystem.INSTANCE.doesItemMatch(filter, new ItemStack(Items.COBBLESTONE), none, reg),
                                "item tasks match through a smart filter");
                    }
                    if (ModList.get().isLoaded("kubejs")) {
                        var server = Minecraft.getInstance().getSingleplayerServer();
                        var uuid = Minecraft.getInstance().player.getUUID();
                        String name = Minecraft.getInstance().player.getGameProfile().getName();
                        server.execute(() -> {
                            var p = server.getPlayerList().getPlayer(uuid);
                            kjs(p).putInt("qc_kjs_count", 0);
                            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "ftbquests change_progress " + name + " reset 00000000000000D7");
                            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "ftbquests change_progress " + name + " complete 00000000000000E7");
                        });
                    }
                    return 40;
                },
                () -> {
                    if (!ModList.get().isLoaded("kubejs")) return 1;
                    var server = Minecraft.getInstance().getSingleplayerServer();
                    var uuid = Minecraft.getInstance().player.getUUID();
                    server.execute(() -> {
                        var d = kjs(server.getPlayerList().getPlayer(uuid));
                        binding = d.getString("qc_kjs_binding");
                        last = d.getString("qc_kjs_last");
                        count = d.getInt("qc_kjs_count");
                        tag = d.getBoolean("qc_kjs_tag");
                        data = d.getBoolean("qc_kjs_data");
                    });
                    return 5;
                },
                () -> {
                    if (!ModList.get().isLoaded("kubejs")) return 1;
                    check(binding != null && binding.startsWith("00000000000000D5"), "FTBQuests.getObject binding: " + binding);
                    check(count == 1, "FTBQuestsEvents.completed('id') fired once: " + count);
                    check(tag, "FTBQuestsEvents.completed('#tag') fired");
                    check(data, "player.data.ftbquests.isCompleted");
                    QuestClustersXMod.LOGGER.info("[devtest] last completed object seen by the script: {}", last);
                    return 1;
                }
        ));
    }
}
