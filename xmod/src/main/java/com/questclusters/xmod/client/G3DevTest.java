package com.questclusters.xmod.client;

import com.questclusters.client.DevSelfTest;
import dev.ftb.mods.ftblibrary.integration.docsmod.DocsModRegistry;
import dev.ftb.mods.ftblibrary.integration.stages.StageHelper;
import dev.ftb.mods.ftbquests.block.entity.BaseBarrierBlockEntity;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/** Stages, docs targets and Jade (G3). */
final class G3DevTest {
    private static final long STAGE_QUEST = 0xA23L;
    private static volatile boolean completedFast;
    private static BlockPos barrierPos;
    private static boolean wasEditing;

    private static void editingMode(boolean on) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        String player = Minecraft.getInstance().player.getGameProfile().getName();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                "ftbquests editing_mode " + on + " " + player));
    }

    private G3DevTest() {
    }

    private static void check(boolean ok, String what) {
        DevSelfTest.moduleCheck(ok, "xmod G3: " + what);
    }

    private static boolean stageQuestDone() {
        var q = ClientQuestFile.INSTANCE.getQuest(STAGE_QUEST);
        return q != null && ClientQuestFile.INSTANCE.selfTeamData.isCompleted(q);
    }

    static List<IntSupplier> steps() {
        List<IntSupplier> steps = new ArrayList<>();
        boolean xmod = ModList.get().isLoaded("ftbxmodcompat");
        boolean kubejs = ModList.get().isLoaded("kubejs");
        steps.add(() -> {
            String name = StageHelper.INSTANCE.getProvider().getName();
            if (!xmod && kubejs) {
                check(name.equals("KubeJS Stages"), "KubeJS stages chosen without XMod: " + name);
            } else {
                DevSelfTest.moduleCheck(true, "xmod G3: stages provider is " + name);
            }
            var server = Minecraft.getInstance().getSingleplayerServer();
            var uuid = Minecraft.getInstance().player.getUUID();
            String player = Minecraft.getInstance().player.getGameProfile().getName();
            server.execute(() -> {
                var p = server.getPlayerList().getPlayer(uuid);
                StageHelper.INSTANCE.getProvider().remove(p, "qc_stage");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        "ftbquests change_progress " + player + " reset 0000000000000A23");
            });
            return 10;
        });
        steps.add(() -> {
            check(!stageQuestDone(), "stage quest starts incomplete");
            var server = Minecraft.getInstance().getSingleplayerServer();
            var uuid = Minecraft.getInstance().player.getUUID();
            server.execute(() -> {
                var p = server.getPlayerList().getPlayer(uuid);
                StageHelper.INSTANCE.getProvider().add(p, "qc_stage");
                // queued right behind the refresh that add() schedules: if that refresh exists, the quest is done
                // here, independent of FTB Quests' once-a-second stage check and of how fast this client runs
                server.execute(() -> completedFast = dev.ftb.mods.ftbquests.quest.ServerQuestFile.INSTANCE.getTeamData(p)
                        .map(d -> d.isCompleted(dev.ftb.mods.ftbquests.quest.ServerQuestFile.INSTANCE.getQuest(STAGE_QUEST)))
                        .orElse(false));
            });
            return 30;
        });
        steps.add(() -> {
            check(stageQuestDone(), "stage task completes when the stage is added");
            if (!xmod && kubejs) {
                check(completedFast, "...right away, not on the next once-a-second check");
            }
            var server = Minecraft.getInstance().getSingleplayerServer();
            var uuid = Minecraft.getInstance().player.getUUID();
            server.execute(() -> StageHelper.INSTANCE.getProvider().remove(server.getPlayerList().getPlayer(uuid), "qc_stage"));
            return 1;
        });
        steps.add(() -> {
            for (String mod : new String[]{"guideme", "patchouli"}) {
                if (ModList.get().isLoaded(mod)) {
                    var target = DocsModRegistry.INSTANCE.getDocsMod(mod);
                    check(target.isPresent() && target.get().getClass().getName().startsWith("com.questclusters"),
                            "\"show docs\" target for " + mod + " is ours: " + target.map(t -> t.getClass().getName()).orElse("none"));
                }
            }
            return 1;
        });
        if (ModList.get().isLoaded("jade")) {
            steps.add(() -> {
                // a stone-skinned quest barrier right in front of the player (edit mode is off in the devtest)
                var mc = Minecraft.getInstance();
                var server = mc.getSingleplayerServer();
                mc.setScreen(null);
                var player = mc.player;
                player.setXRot(0);
                barrierPos = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(2.5)));
                BlockPos pos = barrierPos;
                server.execute(() -> {
                    var level = server.overworld();
                    level.setBlockAndUpdate(pos, ModBlocks.BARRIER.get().defaultBlockState());
                    if (level.getBlockEntity(pos) instanceof BaseBarrierBlockEntity b) {
                        b.setSkin(new ItemStack(Items.MOSSY_COBBLESTONE));
                    }
                });
                return 40;
            });
            steps.add(() -> {
                check(Minecraft.getInstance().level.getBlockEntity(barrierPos) instanceof BaseBarrierBlockEntity b
                        && !b.getSkin().isEmpty(), "skinned barrier placed for the Jade check");
                wasEditing = ClientQuestFile.canClientPlayerEdit();
                editingMode(true);
                return 30;
            });
            steps.add(() -> {
                DevSelfTest.moduleShot("xmod_jade_barrier_editor");
                return 1;
            });
            steps.add(() -> {
                check(ClientQuestFile.canClientPlayerEdit(), "editor view: edit mode on");
                editingMode(false);
                return 30;
            });
            steps.add(() -> {
                DevSelfTest.moduleShot("xmod_jade_barrier_player");
                return 1;
            });
            steps.add(() -> {
                check(!ClientQuestFile.canClientPlayerEdit(), "player view: edit mode off");
                editingMode(wasEditing);
                return 1;
            });
            steps.add(() -> {
                var server = Minecraft.getInstance().getSingleplayerServer();
                BlockPos pos = barrierPos;
                server.execute(() -> server.overworld().removeBlock(pos, false));
                return 5;
            });
        }
        return steps;
    }
}
