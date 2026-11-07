package com.questclusters.client;

import com.questclusters.QuestClusters;
import com.questclusters.client.gui.ClusterBoxWidget;
import com.questclusters.client.gui.ClusterEditor;
import com.questclusters.client.gui.ClusterFrameWidget;
import com.questclusters.data.QuestCluster;
import com.questclusters.net.ClusterPayloads;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * Development-only scripted check of the quest screen integration. Does nothing unless the JVM is started with
 * {@code -Dquest_clusters.devtest=<chapter hex id>} (see the {@code runClientDevtest} Gradle run). Expects the
 * devtest fixture: chapter C1 with quests D0..D3 and cluster F1 = {D1, D2}.
 * Writes screenshots to {@code screenshots/qc_*.png}, results to the log with prefix {@code [devtest]}, then quits.
 * Mouse input is fed through the screen's own click handlers, so no real cursor is needed.
 */
@EventBusSubscriber(modid = QuestClusters.MOD_ID, value = Dist.CLIENT)
public final class DevSelfTest {
    private static final String PROP = System.getProperty("quest_clusters.devtest");
    private static final long CLUSTER = 0xF1L, D0 = 0xD0L, D1 = 0xD1L, D3 = 0xD3L;

    private static final List<IntSupplier> STEPS = new ArrayList<>();
    private static final List<IntSupplier> MODULE_STEPS = new ArrayList<>();
    private static boolean moduleStepsAdded;

    /**
     * Other modules add their checks here during mod construction; they run after core's own steps.
     * A step returns the ticks to wait before the next one, or -1 to be called again next tick.
     */
    public static void addModuleSteps(List<IntSupplier> steps) {
        MODULE_STEPS.addAll(steps);
    }

    /** For module steps: record a check result in the same report. */
    public static void moduleCheck(boolean ok, String what) {
        check(ok, what);
    }

    /** For module steps: the quest screen the self-test is using. */
    public static QuestScreen questScreen() {
        return screen;
    }

    public static boolean active() {
        return PROP != null;
    }
    private static int step = -1;
    private static int wait = 0;
    private static int failures = 0;
    private static QuestScreen screen;
    private static double[] before;
    private static int[] dragFrom;
    private static int waitedForSettlement;

    private DevSelfTest() {
    }

    static {
        // ---------------------------------------------------------------- player view
        STEPS.add(() -> ready() ? 40 : -1);
        STEPS.add(() -> {
            // long run: keep the test player safe from mobs and hunger
            var srv = Minecraft.getInstance().getSingleplayerServer();
            var me = Minecraft.getInstance().player.getUUID();
            log("player health " + Minecraft.getInstance().player.getHealth() + " at " + Minecraft.getInstance().player.blockPosition());
            srv.execute(() -> {
                srv.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
                var sp = srv.getPlayerList().getPlayer(me);
                if (sp != null) {
                    sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                    sp.setHealth(sp.getMaxHealth());
                    // progress lives in the world save and survives runs: start every run from a clean chapter
                    srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack().withSuppressedOutput(),
                            "ftbquests change_progress " + sp.getGameProfile().getName() + " reset " + PROP);
                }
            });
            Chapter ch = ClientQuestFile.INSTANCE.getChapter(QuestCluster.idFromString(PROP));
            check(ch != null, "chapter exists on client");
            // phase C: migration pipeline ran once on load, with a backup first
            var mig = com.questclusters.server.ServerClusters.lastMigration;
            check(mig != null && mig.applied().contains(com.questclusters.migration.DevProbeMigration.ID),
                    "migration ran on first load: " + (mig == null ? null : mig.applied()));
            check(com.questclusters.migration.DevProbeMigration.linesSeen == 3, "migration read the raw chapter keys ("
                    + com.questclusters.migration.DevProbeMigration.linesSeen + " lines)");
            check(mig != null && mig.backup() != null && java.nio.file.Files.exists(mig.backup()), "backup zip made before migrating: "
                    + (mig == null ? null : mig.backup()));
            check(cluster() != null, "cluster synced to client on login");
            screen = ClientQuestFile.openGui();
            screen.open(ch, false);
            ClusterViewState.setExpanded(cluster(), true);
            return 30;
        });
        STEPS.add(() -> {
            report("expanded(default)", false);
            shot("qc_1_default");
            ClusterViewState.setExpanded(cluster(), false);
            screen.questPanel.scrollTo(cluster().x() + 2.5, cluster().y() - 1.5); // keep the box away from the cursor
            return 20;
        });
        STEPS.add(() -> {
            report("collapsed", true);
            for (long m : cluster().members()) {
                check(ClusterViewState.isHidden(m), "member " + QuestCluster.idToString(m) + " hidden when collapsed");
            }
            shot("qc_2_collapsed");
            return 5;
        });
        STEPS.add(() -> {
            check(lines().lastDrawn == 2, "collapsed: 2 merged lines drawn (" + lines().lastDrawn + ")");
            if (!net.neoforged.fml.ModList.get().isLoaded("quest_enhance")) {
                return 0;
            }
            qeHide(D3, 0xD2L, true); // Quest Enhance: hide the real line D3 -> D2 (D2 is inside the collapsed cluster)
            return 25;
        });
        STEPS.add(() -> {
            if (net.neoforged.fml.ModList.get().isLoaded("quest_enhance")) {
                check(lines().lastDrawn == 1, "QE hidden line hides the merged line for players (" + lines().lastDrawn + " drawn)");
            }
            return 0;
        });
        STEPS.add(() -> {
            screen.open((Quest) ClientQuestFile.INSTANCE.getBase(D1), true);
            check(ClusterViewState.isExpanded(cluster()), "open(quest) expands its cluster");
            screen.closeQuest();
            return 20;
        });
        STEPS.add(() -> {
            report("expanded", false);
            shot("qc_3_expanded");
            return 5;
        });

        // ---------------------------------------------------------------- editor
        STEPS.add(() -> {
            PacketDistributor.sendToServer(new ClusterPayloads.Create(chapterId(), "NoPerm", List.of(D0)));
            return 20;
        });
        STEPS.add(() -> {
            check(ClientClusters.store().size() == 1, "server rejects edits outside edit mode");
            var server = Minecraft.getInstance().getSingleplayerServer();
            var uuid = Minecraft.getInstance().player.getUUID();
            server.execute(() -> {
                ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
                server.getPlayerList().op(sp.getGameProfile());
                ServerQuestFile.INSTANCE.getOrCreateTeamData(sp).setPlayerEditMode(sp, true);
            });
            return 40;
        });
        STEPS.add(() -> {
            check(ClientQuestFile.INSTANCE.canEdit(), "edit mode on");
            screen = ClientQuestFile.openGui();
            screen.open(ClientQuestFile.INSTANCE.getChapter(chapterId()), false);
            ClusterViewState.setExpanded(cluster(), false);
            screen.questPanel.scrollTo(cluster().x(), cluster().y() - 2);
            return 20;
        });
        STEPS.add(() -> {
            if (net.neoforged.fml.ModList.get().isLoaded("quest_enhance")) {
                check(lines().lastDrawnHidden == 1, "QE hidden line shown in QE's edit colour in edit mode");
                qeHide(D3, 0xD2L, false);
            }
            return 0;
        });
        STEPS.add(() -> {
            int[] p = centre(box());
            click(p[0], p[1], 0);
            return 10;
        });
        STEPS.add(() -> {
            check(ClusterViewState.isExpanded(cluster()), "left click on box expands (edit mode)");
            return 10;
        });
        STEPS.add(() -> {
            ClusterFrameWidget f = frame();
            int[] p = {(int) (screenX(f) + f.width / 2), (int) (screenY(f) + 3)};
            click(p[0], p[1], 0);
            return 10;
        });
        STEPS.add(() -> {
            check(!ClusterViewState.isExpanded(cluster()), "left click on frame title strip collapses");
            before = new double[]{cluster().x(), cluster().y(), ClientQuestFile.INSTANCE.getQuest(D1).getX()};
            int[] p = centre(box());
            double cell = screen.getQuestButtonSize() + screen.getQuestButtonSpacing();
            press(p[0], p[1], 0);
            release(p[0] + 2 * cell, p[1] + cell, 0);
            return 30;
        });
        STEPS.add(() -> {
            QuestCluster c = cluster();
            check(Math.abs(c.x() - (before[0] + 2)) < 1e-6 && Math.abs(c.y() - (before[1] + 1)) < 1e-6,
                    "drag moves the box by (2,1): now " + c.x() + "," + c.y());
            double qx = ClientQuestFile.INSTANCE.getQuest(D1).getX();
            check(Math.abs(qx - (before[2] + 2)) < 1e-6, "drag moves member quests too: D1.x " + before[2] + " -> " + qx);
            dev.architectury.networking.NetworkManager.sendToServer(dev.ftb.mods.ftbquests.net.UndoRedoRequestMessage.undo());
            return 20;
        });
        STEPS.add(() -> {
            check(Math.abs(cluster().x() - before[0]) < 1e-6 && Math.abs(ClientQuestFile.INSTANCE.getQuest(D1).getX() - before[2]) < 1e-6,
                    "one undo puts box and members back (box " + cluster().x() + ", D1 " + ClientQuestFile.INSTANCE.getQuest(D1).getX() + ")");
            dev.architectury.networking.NetworkManager.sendToServer(dev.ftb.mods.ftbquests.net.UndoRedoRequestMessage.redo());
            return 20;
        });
        STEPS.add(() -> {
            check(Math.abs(cluster().x() - (before[0] + 2)) < 1e-6 && Math.abs(ClientQuestFile.INSTANCE.getQuest(D1).getX() - (before[2] + 2)) < 1e-6,
                    "redo moves them again");
            // ---- the same drag through Minecraft's real mouse handler (hover, press, move over frames, release)
            before = new double[]{cluster().x(), cluster().y(), ClientQuestFile.INSTANCE.getQuest(D1).getX()};
            int[] p = centre(box());
            realMove(p[0], p[1]);
            dragFrom = p;
            return 3;
        });
        STEPS.add(() -> {
            realButton(0, true);
            return 3;
        });
        for (int i = 1; i <= 5; i++) {
            final int k = i;
            STEPS.add(() -> {
                double cell = screen.getQuestButtonSize() + screen.getQuestButtonSpacing();
                realMove(dragFrom[0] + 2 * cell * k / 5D, dragFrom[1] + cell * k / 5D);
                return 2;
            });
        }
        STEPS.add(() -> {
            realButton(0, false);
            return 30;
        });
        STEPS.add(() -> {
            QuestCluster c = cluster();
            check(Math.abs(c.x() - (before[0] + 2)) < 1e-6 && Math.abs(c.y() - (before[1] + 1)) < 1e-6,
                    "real mouse drag moves the box by (2,1): " + before[0] + "," + before[1] + " -> " + c.x() + "," + c.y());
            check(Math.abs(ClientQuestFile.INSTANCE.getQuest(D1).getX() - (before[2] + 2)) < 1e-6, "real mouse drag moves members");
            dev.architectury.networking.NetworkManager.sendToServer(dev.ftb.mods.ftbquests.net.UndoRedoRequestMessage.undo());
            return 20;
        });
        STEPS.add(() -> {
            int[] p = centre(box());
            click(p[0], p[1], 1); // right click: cluster menu
            return 15;
        });
        STEPS.add(() -> {
            shot("qc_4_menu");
            screen.closeContextMenu();
            List<ContextMenuItem> menu = ClusterEditor.clusterMenu(screen, cluster());
            check(menu.size() >= 8, "cluster menu has " + menu.size() + " entries");
            List<ContextMenuItem> objMenu = new ArrayList<>();
            ClusterEditor.addObjectMenuItems(objMenu, screen, ClientQuestFile.INSTANCE.getQuest(D0));
            check(objMenu.size() >= 2, "quest menu gets cluster entries (" + objMenu.size() + ")");
            PacketDistributor.sendToServer(new ClusterPayloads.Create(chapterId(), "Second", List.of(D0)));
            return 20;
        });
        STEPS.add(() -> {
            // real right click on quest D0: goes through FTB Quests' (or another addon's) own menu building
            for (Widget w : screen.questPanel.getWidgets()) {
                if (w instanceof QuestButton qb && qb.moveAndDeleteFocus().getMovableID() == D0 && !(w instanceof dev.ftb.mods.ftbquests.client.gui.quests.QuestLinkButton)) {
                    int[] p = centre(w);
                    click(p[0], p[1], 1);
                }
            }
            return 10;
        });
        STEPS.add(() -> {
            List<String> titles = new ArrayList<>();
            screen.getContextMenu().ifPresent(m -> m.getWidgets().forEach(w -> titles.add(w.getTitle().getString())));
            log("quest menu: " + titles);
            var owner = ClientClusters.store().clusterOfMember(D0);
            String want = owner.isPresent()
                    ? net.minecraft.network.chat.Component.translatable("quest_clusters.menu.remove_from", owner.get().title()).getString()
                    : net.minecraft.network.chat.Component.translatable("quest_clusters.menu.create").getString();
            check(titles.contains(want), "real right-click quest menu contains '" + want + "'");
            shot("qc_7_quest_menu");
            return 5;
        });
        STEPS.add(() -> {
            screen.closeContextMenu();
            return 5;
        });
        STEPS.add(() -> {
            QuestCluster second = ClientClusters.store().clusterOfMember(D0).orElse(null);
            check(second != null && second.title().equals("Second"), "create cluster in edit mode");
            if (second != null) {
                PacketDistributor.sendToServer(new ClusterPayloads.Update(second.withMembers(List.of(D0, D3))));
            }
            return 20;
        });
        STEPS.add(() -> {
            QuestCluster second = ClientClusters.store().clusterOfMember(D3).orElse(null);
            check(second != null && second.members().size() == 2, "add member via update");
            PacketDistributor.sendToServer(new ClusterPayloads.Update(cluster().withMembers(List.of(D1, D0))));
            return 20;
        });
        STEPS.add(() -> {
            check(!cluster().members().contains(D0), "member already in another cluster is rejected");
            QuestCluster second = ClientClusters.store().clusterOfMember(D3).orElseThrow();
            PacketDistributor.sendToServer(new ClusterPayloads.Dissolve(second.id()));
            ClusterViewState.setExpanded(cluster(), true);
            return 20;
        });
        STEPS.add(() -> {
            check(ClientClusters.store().clusterOfMember(D3).isEmpty() && ClientQuestFile.INSTANCE.getQuest(D3) != null,
                    "dissolve removes cluster and keeps quests");
            screen.questPanel.scrollTo(cluster().x(), cluster().y());
            return 20;
        });
        STEPS.add(() -> {
            shot("qc_5_edit_expanded");
            return 5;
        });
        STEPS.add(() -> {
            if (!net.neoforged.fml.ModList.get().isLoaded("quest_enhance")) {
                log("skip copy/paste check (needs Quest Enhance for multi-copy)");
                return 0;
            }
            var acc = (com.questclusters.mixin.client.QuestScreenAccessor) screen;
            acc.quest_clusters$getSelectedObjects().clear();
            for (long m : cluster().members()) acc.quest_clusters$getSelectedObjects().add((dev.ftb.mods.ftbquests.quest.Movable) ClientQuestFile.INSTANCE.getBase(m));
            acc.quest_clusters$invokeCopy();
            return 5;
        });
        STEPS.add(() -> {
            if (!net.neoforged.fml.ModList.get().isLoaded("quest_enhance")) return 0;
            var acc = (com.questclusters.mixin.client.QuestScreenAccessor) screen;
            acc.quest_clusters$getSelectedObjects().clear();
            // paste 4 cells below the cluster (FTB Quests pastes at the canvas position under the mouse)
            var panelAcc = (com.questclusters.mixin.client.QuestPanelAccessor) screen.questPanel;
            panelAcc.quest_clusters$setQuestX(-3);
            panelAcc.quest_clusters$setQuestY(5);
            acc.quest_clusters$invokePaste(false, ClientQuestFile.INSTANCE.getChapter(chapterId()));
            return 40;
        });
        STEPS.add(() -> {
            if (!net.neoforged.fml.ModList.get().isLoaded("quest_enhance")) return 0;
            var pasted = ClientClusters.store().byChapter(chapterId()).stream().filter(c -> c.id() != CLUSTER).toList();
            check(pasted.size() == 1 && pasted.get(0).members().size() == 2
                            && pasted.get(0).members().stream().noneMatch(cluster().members()::contains),
                    "copy/paste with Quest Enhance rebuilds the cluster around the copies (" + pasted + ")");
            return 0;
        });
        STEPS.add(() -> {
            var server = Minecraft.getInstance().getSingleplayerServer();
            server.execute(() -> {
                for (QuestCluster c : com.questclusters.api.QuestClustersAPI.inChapter(QuestCluster.idToString(chapterId()))) {
                    if (c.id() != CLUSTER) com.questclusters.api.QuestClustersAPI.dissolve(c.id());
                }
                com.questclusters.api.QuestClustersAPI.create(QuestCluster.idToString(chapterId()), "{quest_clusters.default_title}",
                        List.of(QuestCluster.idToString(D0)));
            });
            return 20;
        });
        STEPS.add(() -> {
            QuestCluster api = ClientClusters.store().clusterOfMember(D0).orElse(null);
            check(api != null, "API create on the server syncs to the client");
            if (api != null) {
                String shown = com.questclusters.client.gui.ClusterText.title(api).getString();
                check(shown.equals(net.minecraft.network.chat.Component.translatable("quest_clusters.default_title").getString()),
                        "{key} title is translated: " + shown);
            }
            return 0;
        });
        // ---------------------------------------------------------------- cluster gameplay
        STEPS.add(() -> {
            Quest milestone = ClientQuestFile.INSTANCE.getQuest(0xD9L);
            check(milestone != null && !milestone.getTasks().isEmpty()
                    && milestone.getTasks().iterator().next() instanceof com.questclusters.task.ClusterTask, "milestone quest has a cluster task");
            if (milestone != null && !milestone.getTasks().isEmpty()) {
                var task = milestone.getTasks().iterator().next();
                check(task.getMaxProgress() == cluster().members().size(), "cluster task counts the member quests (" + task.getMaxProgress() + ")");
                log("task title: " + task.getTitle().getString());
            }
            // whole-cluster prerequisite: D3 for every member. D3 depends on D2, so D2 must refuse (loop), D1 accepts.
            int changed = ClusterEditor.editMemberDependencies(screen, cluster(), List.of(ClientQuestFile.INSTANCE.getQuest(D3)), true);
            check(changed == 1, "batch prerequisite added to 1 member, loop rejected for the other (" + changed + ")");
            return 20;
        });
        STEPS.add(() -> {
            check(ClientQuestFile.INSTANCE.getQuest(D1).hasDependency(ClientQuestFile.INSTANCE.getQuest(D3)), "D1 now depends on D3 (synced from server)");
            check(!ClientQuestFile.INSTANCE.getQuest(0xD2L).hasDependency(ClientQuestFile.INSTANCE.getQuest(D3)), "D2 does not (would loop)");
            ClusterEditor.editMemberDependencies(screen, cluster(), List.of(ClientQuestFile.INSTANCE.getQuest(D3)), false);
            return 20;
        });
        STEPS.add(() -> {
            check(!ClientQuestFile.INSTANCE.getQuest(D1).hasDependency(ClientQuestFile.INSTANCE.getQuest(D3)), "batch prerequisite removed again");
            var server = Minecraft.getInstance().getSingleplayerServer();
            String name = Minecraft.getInstance().player.getGameProfile().getName();
            server.execute(() -> {
                for (String q : new String[]{"00000000000000D0", "00000000000000D1", "00000000000000D2"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                            "ftbquests change_progress " + name + " complete " + q);
                }
            });
            return 80; // the task re-checks every 20 ticks
        });
        STEPS.add(() -> {
            var td = ClientQuestFile.INSTANCE.selfTeamData;
            Quest milestone = ClientQuestFile.INSTANCE.getQuest(0xD9L);
            check(td.isCompleted(ClientQuestFile.INSTANCE.getQuest(D1)) && td.isCompleted(ClientQuestFile.INSTANCE.getQuest(0xD2L)), "members completed");
            check(milestone != null && td.isCompleted(milestone), "milestone completes when the whole cluster is done");
            check(milestone != null && td.hasUnclaimedRewards(Minecraft.getInstance().player.getUUID(), milestone), "milestone reward is claimable");
            return 5;
        });
        // ---------------------------------------------------------------- phase B: scoring, settlement page, leaderboard
        STEPS.add(() -> {
            check(com.questclusters.client.ClientScoring.entries().size() == 4, "scoring data synced (" + com.questclusters.client.ClientScoring.entries().size() + ")");
            var r = com.questclusters.client.ClientScoring.myResult();
            check(r.total() == 60 && r.max() == 60, "team score computed from completions: " + r.total() + "/" + r.max());
            check(r.nodes().size() == 3, "completed nodes listed: " + r.nodes().stream().map(com.questclusters.data.ScoreCalc.Node::text).toList());
            for (long q : new long[]{D0, D1, 0xD2L, 0xD9L}) {
                log("completed time " + QuestCluster.idToString(q) + " client=" + ClientQuestFile.INSTANCE.selfTeamData.getCompletedTime(q));
            }
            check(com.questclusters.client.ClientScoring.settings().rankFor(r.percent()).equals("&6S"), "rank from percentage");
            return 0;
        });
        STEPS.add(() -> {
            // completing the milestone (opens_settlement) must have opened the settlement page by now, or within a few ticks
            if (!(Minecraft.getInstance().screen instanceof com.questclusters.client.gui.SettlementScreen) && waitedForSettlement++ < 100) {
                return -1;
            }
            check(Minecraft.getInstance().screen instanceof com.questclusters.client.gui.SettlementScreen,
                    "settlement page opened by completing the milestone (after " + waitedForSettlement + " ticks)");
            if (Minecraft.getInstance().screen instanceof com.questclusters.client.gui.SettlementScreen ss) {
                var lines = ss.plainLines();
                log("settlement lines: " + lines);
                check(lines.contains("Test Journey") && lines.contains("第一步") && lines.contains("Second step") && lines.contains("Milestone reached"),
                        "settlement page shows title and node texts in order");
                check(lines.stream().anyMatch(l -> l.contains("60") && l.contains("100")), "settlement page shows total and percentage");
                check(lines.contains("S") && lines.contains("The end"), "settlement page shows rank and closing line");
            }
            return 60; // let it scroll a little
        });
        STEPS.add(() -> {
            shot("qc_15_settlement");
            Minecraft.getInstance().screen.onClose();
            PacketDistributor.sendToServer(new ClusterPayloads.RequestLeaderboard());
            return 20;
        });
        STEPS.add(() -> {
            check(Minecraft.getInstance().screen instanceof com.questclusters.client.gui.LeaderboardScreen, "leaderboard screen opens");
            if (Minecraft.getInstance().screen instanceof com.questclusters.client.gui.LeaderboardScreen ls) {
                var own = ls.rows().stream().filter(com.questclusters.data.ScoreCalc.Row::own).findFirst().orElse(null);
                log("leaderboard: " + ls.rows());
                check(own != null && own.score() == 60 && own.quests() >= 3, "own team on the leaderboard with its score");
            }
            shot("qc_16_leaderboard");
            return 5;
        });
        STEPS.add(() -> {
            Minecraft.getInstance().screen.onClose();
            screen = ClientQuestFile.openGui();
            screen.open(ClientQuestFile.INSTANCE.getChapter(chapterId()), false);
            check(screen.otherButtonsTopPanel.getWidgets().stream().anyMatch(w -> w.getClass().getSimpleName().equals("SettlementButton")),
                    "settlement button in the quest book");
            return 10;
        });
        // ---------------------------------------------------------------- undo of "delete cluster and members"
        STEPS.add(() -> {
            PacketDistributor.sendToServer(new ClusterPayloads.Create(chapterId(), "Temp", List.of(D3)));
            return 20;
        });
        STEPS.add(() -> {
            QuestCluster temp = ClientClusters.store().clusterOfMember(D3).orElse(null);
            check(temp != null, "temp cluster created");
            if (temp != null) {
                ClusterEditor.clusterMenu(screen, temp); // builds fine
                dev.architectury.networking.NetworkManager.sendToServer(new dev.ftb.mods.ftbquests.net.DeleteObjectMessage(List.copyOf(temp.members())));
            }
            return 30;
        });
        STEPS.add(() -> {
            check(ClientQuestFile.INSTANCE.getQuest(D3) == null && ClientClusters.store().clusterOfMember(D3).isEmpty(),
                    "delete with members removes quest and cluster");
            dev.architectury.networking.NetworkManager.sendToServer(dev.ftb.mods.ftbquests.net.UndoRedoRequestMessage.undo());
            return 30;
        });
        STEPS.add(() -> {
            QuestCluster back = ClientClusters.store().clusterOfMember(D3).orElse(null);
            check(ClientQuestFile.INSTANCE.getQuest(D3) != null, "undo restores the deleted quest");
            check(back != null && back.title().equals("Temp"), "undo also restores its cluster (" + back + ")");
            return 0;
        });
        STEPS.add(() -> {
            screen.questPanel.scrollTo(cluster().x(), cluster().y());
            ClusterViewState.setExpanded(cluster(), false);
            return 20;
        });
        STEPS.add(() -> {
            shot("qc_8_lines");
            ClusterViewState.setExpanded(cluster(), true);
            return 5;
        });
        // ---------------------------------------------------------------- phase A
        // A1) Quest Enhance decorative lines: inside anchor moves and hides with the cluster, outside lines reroute
        STEPS.add(() -> {
            if (!net.neoforged.fml.ModList.get().isLoaded("quest_enhance")) {
                log("skip decorative-line checks (needs Quest Enhance)");
                return 0;
            }
            var ch = ClientQuestFile.INSTANCE.getChapter(chapterId());
            var inside = com.questclusters.compat.QuestEnhanceCompat.anchorsInside(ch, cluster().members());
            check(inside.contains(0xA1L), "anchor serving only cluster lines is detected: " + inside);
            before = new double[]{((dev.ftb.mods.ftbquests.quest.Movable) ClientQuestFile.INSTANCE.getBase(0xA1L)).getX()};
            PacketDistributor.sendToServer(new ClusterPayloads.Move(CLUSTER, 1, 0));
            return 25;
        });
        STEPS.add(() -> {
            if (!net.neoforged.fml.ModList.get().isLoaded("quest_enhance")) return 0;
            double ax = ((dev.ftb.mods.ftbquests.quest.Movable) ClientQuestFile.INSTANCE.getBase(0xA1L)).getX();
            check(Math.abs(ax - (before[0] + 1)) < 1e-6, "anchor moves with the cluster: " + before[0] + " -> " + ax);
            dev.architectury.networking.NetworkManager.sendToServer(dev.ftb.mods.ftbquests.net.UndoRedoRequestMessage.undo());
            return 25;
        });
        STEPS.add(() -> {
            if (!net.neoforged.fml.ModList.get().isLoaded("quest_enhance")) return 0;
            double ax = ((dev.ftb.mods.ftbquests.quest.Movable) ClientQuestFile.INSTANCE.getBase(0xA1L)).getX();
            check(Math.abs(ax - before[0]) < 1e-6, "one undo puts the anchor back too");
            ClusterViewState.setExpanded(cluster(), false);
            com.questclusters.client.gui.DecorativeLineWindow.swaps = 0;
            screen.questPanel.scrollTo(cluster().x(), cluster().y());
            return 15;
        });
        STEPS.add(() -> {
            if (!net.neoforged.fml.ModList.get().isLoaded("quest_enhance")) return 0;
            check(ClusterViewState.isHiddenImage(0xA1L), "inside anchor hidden while collapsed");
            if (net.neoforged.fml.ModList.get().isLoaded("certain_questing_additions")) {
                // CQA cancels the whole line pass, so Quest Enhance draws no decorative lines at all (upstream conflict)
                log("CQA present: Quest Enhance decorative lines are not drawn by upstream; swaps=" + com.questclusters.client.gui.DecorativeLineWindow.swaps);
            } else {
                check(com.questclusters.client.gui.DecorativeLineWindow.swaps > 0,
                        "Quest Enhance's decorative lines see the box instead of hidden quests (" + com.questclusters.client.gui.DecorativeLineWindow.swaps + " frames)");
            }
            check(screen.questPanel.getWidgets().stream().noneMatch(w -> w.getClass().getSimpleName().equals("Stand")),
                    "stand-ins never leak outside the drawing window");
            shot("qc_11_decorative_collapsed");
            ClusterViewState.setExpanded(cluster(), true);
            return 5;
        });
        // A2) background image
        STEPS.add(() -> {
            PacketDistributor.sendToServer(new ClusterPayloads.Update(cluster().withStyle(cluster().style()
                    .withBackground("minecraft:textures/block/oak_planks.png", com.questclusters.data.ClusterStyle.MODE_TILE, 4, true))));
            return 20;
        });
        STEPS.add(() -> {
            check(cluster().style().background().equals("minecraft:textures/block/oak_planks.png")
                    && cluster().style().backgroundMode() == com.questclusters.data.ClusterStyle.MODE_TILE, "background image saved and synced");
            ClusterViewState.setExpanded(cluster(), false);
            screen.questPanel.scrollTo(cluster().x(), cluster().y() + 1.5); // box above the edit-mode change log
            return 15;
        });
        STEPS.add(() -> {
            shot("qc_12_background_tile");
            PacketDistributor.sendToServer(new ClusterPayloads.Update(cluster().withStyle(cluster().style()
                    .withBackground("minecraft:textures/gui/sprites/widget/button.png", com.questclusters.data.ClusterStyle.MODE_NINE_SLICE, 3, true))));
            return 20;
        });
        STEPS.add(() -> {
            check(cluster().style().backgroundMode() == com.questclusters.data.ClusterStyle.MODE_NINE_SLICE, "nine-slice mode synced");
            return 5;
        });
        STEPS.add(() -> {
            shot("qc_13_background_nine_slice");
            ClusterViewState.setExpanded(cluster(), true);
            return 5;
        });
        // A3) chapter search
        STEPS.add(() -> {
            var results = com.questclusters.client.gui.QuestSearch.search("milest");
            check(results.stream().anyMatch(r -> r.quest() != null && r.quest().id == 0xD9L), "search finds quest by title: " + results.size());
            var clusterHits = com.questclusters.client.gui.QuestSearch.search("矿石");
            check(clusterHits.stream().anyMatch(r -> r.cluster() != null && r.cluster().id() == CLUSTER), "search finds the cluster by title");
            check(com.questclusters.client.gui.QuestSearch.search("zzzz-nothing").isEmpty(), "no false matches");
            var panel = ((com.questclusters.mixin.client.QuestScreenAccessor) screen).quest_clusters$getChapterPanel();
            check(com.questclusters.client.gui.ChapterSearchWidgets.focus(panel), "search box exists and can be focused (Ctrl+F)");
            realMove(30, 60); // keep the chapter panel open under the cursor
            panel.getWidgets().stream().filter(w -> w instanceof dev.ftb.mods.ftblibrary.ui.TextBox).findFirst()
                    .ifPresent(w -> ((dev.ftb.mods.ftblibrary.ui.TextBox) w).setText("Milest")); // may refresh the panel
            return 10;
        });
        STEPS.add(() -> {
            var panel = ((com.questclusters.mixin.client.QuestScreenAccessor) screen).quest_clusters$getChapterPanel();
            Widget hit = null;
            int results = 0;
            for (Widget w : panel.getWidgets()) {
                if (w.getClass().getSimpleName().equals("ResultButton")) {
                    results++;
                    if (hit == null) hit = w;
                }
                check(!(w instanceof dev.ftb.mods.ftbquests.client.gui.quests.ChapterPanel.ChapterButton), "chapter list replaced by results");
            }
            check(results >= 1, "result buttons shown (" + results + ")");
            check(com.questclusters.client.gui.ChapterSearchWidgets.isSearchFocused(panel), "search box keeps focus while typing");
            check(((com.questclusters.mixin.client.ChapterPanelAccessor) panel).quest_clusters$isExpanded(), "chapter panel stays open while searching");
            shot("qc_14_search");
            if (hit instanceof dev.ftb.mods.ftblibrary.ui.Button b) {
                b.onClicked(dev.ftb.mods.ftblibrary.ui.input.MouseButton.LEFT);
            }
            return 10;
        });
        STEPS.add(() -> {
            check(screen.getViewedQuest() != null && screen.getViewedQuest().id == 0xD9L, "clicking a result opens the quest");
            screen.closeQuest();
            var panel = ((com.questclusters.mixin.client.QuestScreenAccessor) screen).quest_clusters$getChapterPanel();
            panel.getWidgets().stream().filter(w -> w instanceof dev.ftb.mods.ftblibrary.ui.TextBox).findFirst()
                    .ifPresent(w -> ((dev.ftb.mods.ftblibrary.ui.TextBox) w).setText("")); // may refresh the panel
            return 5;
        });
        STEPS.add(() -> {
            var panel = ((com.questclusters.mixin.client.QuestScreenAccessor) screen).quest_clusters$getChapterPanel();
            check(panel.getWidgets().stream().anyMatch(w -> w instanceof dev.ftb.mods.ftbquests.client.gui.quests.ChapterPanel.ChapterButton),
                    "clearing the search brings the chapter list back");
            return 5;
        });

        // phase C: a reload must not run the migration again
        STEPS.add(() -> {
            var srv = Minecraft.getInstance().getSingleplayerServer();
            srv.execute(() -> srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack().withSuppressedOutput(), "ftbquests reload"));
            return 40;
        });
        STEPS.add(() -> {
            var mig = com.questclusters.server.ServerClusters.lastMigration;
            check(mig != null && !mig.didSomething() && mig.backup() == null, "reload: already-applied migration not repeated, no new backup");
            check(ClientClusters.store().get(CLUSTER).isPresent(), "clusters still synced after /ftbquests reload");
            screen = ClientQuestFile.openGui();
            screen.open(ClientQuestFile.INSTANCE.getChapter(chapterId()), false);
            return 10;
        });

        // ---------------------------------------------------------------- user reports (v0.1.0)
        // 1) Esc / E after the editor must go back to the quest screen, then out to the game
        for (int key : new int[]{256, 69}) { // GLFW_KEY_ESCAPE, GLFW_KEY_E
            STEPS.add(() -> {
                Minecraft.getInstance().setScreen(null); // like a player: open the quest book from the game
                return 5;
            });
            STEPS.add(() -> {
                screen = ClientQuestFile.openGui();
                return 10;
            });
            STEPS.add(() -> {
                ClusterEditor.openEditor(screen, cluster());
                return 10;
            });
            STEPS.add(() -> {
                realKey(256); // Esc closes the editor (cancel)
                return 10;
            });
            STEPS.add(() -> {
                check(dev.ftb.mods.ftblibrary.util.client.ClientUtils.getCurrentGuiAs(QuestScreen.class) != null,
                        "closing the editor returns to the quest screen");
                realKey(key);
                return 10;
            });
            STEPS.add(() -> {
                check(Minecraft.getInstance().screen == null, (key == 256 ? "Esc" : "E") + " then closes the quest book (screen now "
                        + Minecraft.getInstance().screen + ")");
                return 5;
            });
        }
        STEPS.add(() -> {
            screen = ClientQuestFile.openGui();
            screen.open(ClientQuestFile.INSTANCE.getChapter(chapterId()), false);
            // 2) FTB text formats
            var gold = com.questclusters.client.gui.ClusterText.text("&6Gold");
            var json = com.questclusters.client.gui.ClusterText.text("{\"text\":\"JSON\",\"color\":\"red\"}");
            log("parsed: " + gold + " | " + json);
            check(gold.getString().equals("Gold") && hasColor(gold, net.minecraft.ChatFormatting.GOLD), "& colour codes are parsed");
            check(json.getString().equals("JSON") && hasColor(json, net.minecraft.ChatFormatting.RED), "JSON text is parsed");
            // 3) colours are stored and synced
            PacketDistributor.sendToServer(new ClusterPayloads.Update(cluster().withStyle(
                    new com.questclusters.data.ClusterStyle(0xFF00FF00, 0xFFFF00FF, 0xFFFF0000, 0xFF123456))));
            return 20;
        });
        STEPS.add(() -> {
            var st = cluster().style();
            check(st.titleColor() == 0xFF00FF00 && st.borderColor() == 0xFFFF0000 && st.frameColor() == 0xFF123456, "custom colours saved and synced: " + st);
            // 4) moving all members with FTB Quests' own move request: box follows
            before = new double[]{cluster().x(), cluster().y()};
            var ch = ClientQuestFile.INSTANCE.getChapter(chapterId());
            for (long m : cluster().members()) {
                var mv = (dev.ftb.mods.ftbquests.quest.Movable) ClientQuestFile.INSTANCE.getBase(m);
                dev.architectury.networking.NetworkManager.sendToServer(new dev.ftb.mods.ftbquests.net.MoveMovableMessage(m, ch.id, mv.getX() + 1, mv.getY()));
            }
            return 40;
        });
        STEPS.add(() -> {
            check(Math.abs(cluster().x() - (before[0] + 1)) < 1e-6 && Math.abs(cluster().y() - before[1]) < 1e-6,
                    "box follows when all members are moved with FTB Quests' move: " + before[0] + " -> " + cluster().x());
            // 5) drag an expanded frame by its left edge with the real mouse
            ClusterViewState.setExpanded(cluster(), true);
            double cx = 0, cy = 0;
            for (long m : cluster().members()) {
                var mv = (dev.ftb.mods.ftbquests.quest.Movable) ClientQuestFile.INSTANCE.getBase(m);
                cx += mv.getX() / cluster().members().size();
                cy += mv.getY() / cluster().members().size();
            }
            screen.questPanel.scrollTo(cx, cy); // frame in the middle of the screen
            return 15;
        });
        STEPS.add(() -> {
            before = new double[]{cluster().x(), cluster().y()};
            ClusterFrameWidget f = frame();
            dragFrom = new int[]{(int) (screenX(f) + f.width - 3), (int) (screenY(f) + f.height - 2)}; // bottom edge, right end
            realMove(dragFrom[0], dragFrom[1]);
            return 3;
        });
        STEPS.add(() -> {
            ClusterFrameWidget f = frame();
            log("frame drag: frame at " + screenX(f) + "," + screenY(f) + " " + f.width + "x" + f.height + ", press at "
                    + dragFrom[0] + "," + dragFrom[1] + ", mouse " + screen.getMouseX() + "," + screen.getMouseY()
                    + ", over=" + f.isMouseOver() + ", draw=" + f.shouldDraw());
            realButton(0, true);
            return 3;
        });
        for (int i = 1; i <= 4; i++) {
            final int k = i;
            STEPS.add(() -> {
                double cell = screen.getQuestButtonSize() + screen.getQuestButtonSpacing();
                realMove(dragFrom[0], dragFrom[1] + cell * k / 4D);
                return 2;
            });
        }
        STEPS.add(() -> {
            realButton(0, false);
            return 30;
        });
        STEPS.add(() -> {
            check(Math.abs(cluster().y() - (before[1] + 1)) < 1e-6, "dragging the expanded frame's edge moves the cluster: "
                    + before[1] + " -> " + cluster().y());
            shot("qc_9_colours");
            return 5;
        });
        STEPS.add(() -> {
            ClusterEditor.openEditor(screen, cluster());
            return 15;
        });
        STEPS.add(() -> {
            check(dev.ftb.mods.ftblibrary.util.client.ClientUtils.getCurrentGuiAs(
                    dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen.class) != null, "edit screen opens");
            shot("qc_6_edit_screen");
            return 5;
        });
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (PROP == null || step == Integer.MAX_VALUE) {
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!moduleStepsAdded) {
                moduleStepsAdded = true;
                STEPS.addAll(STEPS.size() - 2 >= 0 ? STEPS.size() : 0, MODULE_STEPS);
            }
            int idx = step < 0 ? 0 : step;
            if (idx >= STEPS.size()) {
                log(failures == 0 ? "ALL CHECKS PASSED" : failures + " CHECK(S) FAILED");
                step = Integer.MAX_VALUE;
                mc.stop();
                return;
            }
            int w = STEPS.get(idx).getAsInt();
            if (w >= 0) {
                step = idx + 1;
                wait = w;
            }
        } catch (Throwable t) {
            QuestClusters.LOGGER.error("[devtest] crashed at step {}", step, t);
            step = Integer.MAX_VALUE;
            mc.stop();
        }
    }

    // ---------------------------------------------------------------- helpers

    private static boolean ready() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && ClientQuestFile.exists() && ClientQuestFile.INSTANCE.selfTeamData != null
                && ClientClusters.store().size() > 0;
    }

    private static long chapterId() {
        return QuestCluster.idFromString(PROP);
    }

    private static QuestCluster cluster() {
        return ClientClusters.store().get(CLUSTER).orElse(null);
    }

    private static <T> T find(Class<T> type) {
        for (Widget w : screen.questPanel.getWidgets()) {
            if (type.isInstance(w)) {
                return type.cast(w);
            }
        }
        throw new IllegalStateException("no " + type.getSimpleName() + " in quest panel");
    }

    private static com.questclusters.client.gui.ClusterLinesWidget lines() {
        return find(com.questclusters.client.gui.ClusterLinesWidget.class);
    }

    /** Toggle a Quest Enhance hidden line on the client copy of the chapter (reflection; QE is optional). */
    private static void qeHide(long dependent, long dependency, boolean hidden) {
        try {
            Class<?> c = Class.forName("com.quest_enhance.HiddenDependencyLines");
            var nodeKey = c.getMethod("nodeKey", dev.ftb.mods.ftbquests.quest.Movable.class);
            var set = c.getMethod("set", Chapter.class, String.class, String.class, boolean.class, boolean.class);
            var file = ClientQuestFile.INSTANCE;
            set.invoke(null, file.getChapter(chapterId()), nodeKey.invoke(null, file.getBase(dependent)),
                    nodeKey.invoke(null, file.getBase(dependency)), hidden, false);
        } catch (Throwable t) {
            check(false, "could not drive Quest Enhance hidden lines: " + t);
        }
    }

    /** Mouse move through Minecraft's own input path (GUI coordinates in). */
    private static void realMove(double gx, double gy) {
        Minecraft mc = Minecraft.getInstance();
        var w = mc.getWindow();
        double sx = (double) w.getScreenWidth() / w.getGuiScaledWidth();
        double sy = (double) w.getScreenHeight() / w.getGuiScaledHeight();
        try {
            var m = net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
            m.setAccessible(true);
            m.invoke(mc.mouseHandler, w.getWindow(), gx * sx, gy * sy);
        } catch (Throwable t) {
            check(false, "realMove: " + t);
        }
    }

    private static void realButton(int button, boolean down) {
        Minecraft mc = Minecraft.getInstance();
        try {
            var m = net.minecraft.client.MouseHandler.class.getDeclaredMethod("onPress", long.class, int.class, int.class, int.class);
            m.setAccessible(true);
            m.invoke(mc.mouseHandler, mc.getWindow().getWindow(), button, down ? 1 : 0, 0);
        } catch (Throwable t) {
            check(false, "realButton: " + t);
        }
    }

    private static boolean hasColor(net.minecraft.network.chat.Component c, net.minecraft.ChatFormatting f) {
        int want = f.getColor();
        boolean[] found = {false};
        c.visit((style, text) -> {
            if (style.getColor() != null && style.getColor().getValue() == want && !text.isEmpty()) found[0] = true;
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return found[0];
    }

    private static void realKey(int glfwKey) {
        Minecraft mc = Minecraft.getInstance();
        long w = mc.getWindow().getWindow();
        mc.keyboardHandler.keyPress(w, glfwKey, 0, 1, 0);
        mc.keyboardHandler.keyPress(w, glfwKey, 0, 0, 0);
    }

    /** Widgets of the fixture cluster F1 (other clusters exist later in the test). */
    private static ClusterBoxWidget box() {
        for (Widget w : screen.questPanel.getWidgets()) {
            if (w instanceof ClusterBoxWidget b && b.clusterId() == CLUSTER) return b;
        }
        throw new IllegalStateException("no box for F1");
    }

    private static ClusterFrameWidget frame() {
        for (Widget w : screen.questPanel.getWidgets()) {
            if (w instanceof ClusterFrameWidget f && f.clusterId() == CLUSTER) return f;
        }
        throw new IllegalStateException("no frame for F1");
    }

    private static double screenX(Widget w) {
        return screen.questPanel.getX() - screen.questPanel.getScrollX() + w.posX;
    }

    private static double screenY(Widget w) {
        return screen.questPanel.getY() - screen.questPanel.getScrollY() + w.posY;
    }

    private static int[] centre(Widget w) {
        return new int[]{(int) (screenX(w) + w.width / 2D), (int) (screenY(w) + w.height / 2D)};
    }

    private static net.minecraft.client.gui.screens.Screen wrapper() {
        return Minecraft.getInstance().screen;
    }

    // FTB Library only refreshes its stored mouse position while rendering (BaseScreen#updateGui),
    // so feed it before each synthetic event, as a real frame would
    private static void press(double x, double y, int button) {
        screen.updateGui((int) x, (int) y, 0F);
        wrapper().mouseClicked(x, y, button);
    }

    private static void release(double x, double y, int button) {
        screen.updateGui((int) x, (int) y, 0F);
        wrapper().mouseReleased(x, y, button);
    }

    private static void click(double x, double y, int button) {
        press(x, y, button);
        release(x, y, button);
    }

    private static void report(String label, boolean collapsed) {
        int quests = 0, hidden = 0, boxesShown = 0, framesShown = 0;
        for (Widget w : screen.questPanel.getWidgets()) {
            if (w instanceof QuestButton qb) {
                quests++;
                if (!qb.shouldDraw()) hidden++;
            } else if (w instanceof ClusterBoxWidget b && b.shouldDraw()) {
                boxesShown++;
            } else if (w instanceof ClusterFrameWidget f && f.shouldDraw()) {
                framesShown++;
            }
        }
        log(label + ": quest buttons " + quests + " (hidden " + hidden + "), boxes shown " + boxesShown + ", frames shown " + framesShown);
        check(boxesShown == (collapsed ? 1 : 0) && framesShown == (collapsed ? 0 : 1), label + ": box/frame visibility");
        check(hidden == (collapsed ? cluster().members().size() : 0), label + ": hidden member count");
    }

    public static void moduleShot(String name) {
        shot(name);
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> log("screenshot " + name));
    }

    private static void check(boolean ok, String what) {
        if (!ok) failures++;
        log((ok ? "PASS " : "FAIL ") + what);
    }

    private static void log(String s) {
        QuestClusters.LOGGER.info("[devtest] {}", s);
    }
}
