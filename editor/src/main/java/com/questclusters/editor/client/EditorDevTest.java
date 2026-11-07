package com.questclusters.editor.client;

import com.questclusters.client.DevSelfTest;
import com.questclusters.editor.client.gui.DecorativeLinesWidget;
import com.questclusters.editor.data.BookExtras;
import com.questclusters.editor.migration.QuestEnhanceMigration;
import com.questclusters.editor.mixin.client.QuestScreenEditorAccessor;
import com.questclusters.migration.MigrationRunner;
import com.questclusters.mixin.client.QuestScreenAccessor;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.IntSupplier;

/** Editor checks for the dev self-test (fixture chapters C2 "editor_test" and C3 "editor_locked"). */
final class EditorDevTest {
    private static final long C1 = 0xC1L, C2 = 0xC2L, C3 = 0xC3L, D5 = 0xD5L, D6 = 0xD6L, A6 = 0xA6L, A7 = 0xA7L;
    private static volatile Boolean serverLocked;

    private EditorDevTest() {
    }

    private static void editMode(boolean on) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        var uuid = Minecraft.getInstance().player.getUUID();
        server.execute(() -> {
            var sp = server.getPlayerList().getPlayer(uuid);
            ServerQuestFile.INSTANCE.getOrCreateTeamData(sp).setPlayerEditMode(sp, on);
            serverLocked = !ServerQuestFile.INSTANCE.getChapter(C3).isVisible(ServerQuestFile.INSTANCE.getOrCreateTeamData(sp));
        });
    }

    private static void command(String cmd) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        String name = Minecraft.getInstance().player.getGameProfile().getName();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd.replace("@me", name)));
    }

    private static void counters() {
        EditorDebug.hiddenPurple = EditorDebug.hiddenSkipped = EditorDebug.hiddenRevealed = 0;
    }

    static void register() {
        DevSelfTest.addModuleSteps(List.<IntSupplier>of(
                () -> {
                    Path quests = FMLPaths.CONFIGDIR.get().resolve("ftbquests/quests");
                    DevSelfTest.moduleCheck(MigrationRunner.applied(quests).containsKey(QuestEnhanceMigration.ID), "editor: Quest Enhance import recorded");
                    DevSelfTest.moduleCheck(Files.exists(quests.resolve("quest_clusters/editor/00000000000000C2.snbt"))
                            && Files.exists(quests.resolve("quest_clusters/editor/book.snbt")), "editor: own data files written");
                    BookExtras b = ClientEditor.book();
                    DevSelfTest.moduleCheck(b.sidebarBackground.endsWith("dark_oak_planks.png"), "editor: sidebar background synced");
                    DevSelfTest.moduleCheck(b.chapter(C2).background.endsWith("deepslate_tiles.png"), "editor: chapter background synced");
                    DevSelfTest.moduleCheck(b.quest(D5).boxBackground().endsWith("gold_block.png")
                            && b.quest(D5).viewBackground().endsWith("sculk.png") && b.quest(D5).disableScrollPaging(), "editor: quest backgrounds + scroll paging synced");
                    DevSelfTest.moduleCheck(b.chapter(C1).decorativeLines.size() == 2 && b.chapter(C2).decorativeLines.size() == 1, "editor: decorative lines imported");
                    DevSelfTest.moduleCheck(b.chapter(C2).hiddenLines.size() == 1 && b.chapter(C2).hiddenLines.get(0).revealOnHover(), "editor: hidden line imported");
                    DevSelfTest.moduleCheck(b.chapter(C3).prerequisites.equals(List.of(D6)), "editor: chapter prerequisites imported");
                    DevSelfTest.moduleCheck(b.excludedImages.contains(A6), "editor: box-select exclusion imported");
                    DevSelfTest.moduleCheck(ClientQuestFile.INSTANCE.getBase(A7) instanceof ChapterImage img
                            && img.getClickAction().actionData().startsWith("quest_enhance:text:"), "editor: legacy 'click' canvas text restored");
                    DevSelfTest.moduleCheck(ClientQuestFile.INSTANCE.getChapter(C3).isVisible(ClientQuestFile.INSTANCE.selfTeamData),
                            "editor: locked chapter visible in edit mode");
                    command("ftbquests change_progress @me reset 00000000000000D6"); // earlier runs completed it
                    editMode(false);
                    return 40;
                },
                () -> {
                    var file = ClientQuestFile.INSTANCE;
                    DevSelfTest.moduleCheck(!file.canEdit(), "editor: player mode");
                    DevSelfTest.moduleCheck(!file.getChapter(C3).isVisible(file.selfTeamData), "editor: chapter hidden until its prerequisite is done (client)");
                    DevSelfTest.moduleCheck(Boolean.TRUE.equals(serverLocked), "editor: ... and on the server");
                    var qs = ClientQuestFile.openGui();
                    qs.open(file.getChapter(C2), false);
                    qs.questPanel.scrollTo(0, 0.5);
                    counters();
                    return 30;
                },
                () -> {
                    DevSelfTest.moduleCheck(DecorativeLinesWidget.lastDrawn == 2, "editor: decorative line drawn as 2 spokes (" + DecorativeLinesWidget.lastDrawn + ")");
                    DevSelfTest.moduleCheck(EditorDebug.hiddenSkipped > 0 && EditorDebug.hiddenPurple == 0, "editor: hidden line not drawn for players");
                    DevSelfTest.moduleShot("qc_30_editor_player");
                    DevSelfTest.questScreen().open((Quest) ClientQuestFile.INSTANCE.getBase(D5), false);
                    return 20;
                },
                () -> {
                    DevSelfTest.moduleCheck(DevSelfTest.questScreen().isViewingQuest(), "editor: details view open");
                    DevSelfTest.moduleShot("qc_31_editor_details");
                    command("ftbquests change_progress @me complete 00000000000000D6");
                    return 40;
                },
                () -> {
                    var file = ClientQuestFile.INSTANCE;
                    DevSelfTest.moduleCheck(file.getChapter(C3).isVisible(file.selfTeamData), "editor: chapter unlocks when its prerequisite is completed");
                    editMode(true);
                    return 40;
                },
                () -> {
                    var qs = ClientQuestFile.openGui();
                    qs.open(ClientQuestFile.INSTANCE.getChapter(C2), false);
                    qs.questPanel.scrollTo(0, 0.5);
                    counters();
                    return 30;
                },
                () -> {
                    var qs = DevSelfTest.questScreen();
                    DevSelfTest.moduleCheck(EditorDebug.hiddenPurple > 0, "editor: hidden line drawn purple in edit mode");
                    DevSelfTest.moduleShot("qc_32_editor_edit");
                    var sel = ((QuestScreenAccessor) qs).quest_clusters$getSelectedObjects();
                    sel.clear();
                    var acc = (QuestScreenEditorAccessor) qs;
                    acc.quest_clusters_editor$setPrevMouseX(-100000);
                    acc.quest_clusters_editor$setPrevMouseY(-100000);
                    acc.quest_clusters_editor$selectAllInBox(100000, 100000, qs.questPanel.getScrollX(), qs.questPanel.getScrollY());
                    boolean hasQuest = sel.stream().anyMatch(m -> m.getMovableID() == D5);
                    boolean hasText = sel.stream().anyMatch(m -> m.getMovableID() == A6);
                    boolean hasOther = sel.stream().anyMatch(m -> m.getMovableID() == A7);
                    DevSelfTest.moduleCheck(hasQuest && hasOther && !hasText, "editor: box selection skips excluded images");
                    sel.clear();
                    return 5;
                }
        ));
    }
}
