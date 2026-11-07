package com.questclusters.editor.client;

import com.questclusters.client.DevSelfTest;
import com.questclusters.editor.QuestClustersEditor;
import com.questclusters.editor.client.gui.DecorativeLinesWidget;
import com.questclusters.editor.data.CanvasElement;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Dev only ({@code -Dquest_clusters_editor.tour=true}): opens every chapter of whatever quest book is loaded,
 * screenshots it ({@code screenshots/tour_<n>.png}), logs what the editor drew, then quits. For checking imported
 * books by eye.
 */
final class EditorTour {
    private static int wait = 100, index = -1;
    private static List<Chapter> chapters;
    private static QuestScreen screen;

    private EditorTour() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (index == Integer.MAX_VALUE || mc.player == null || ClientQuestFile.INSTANCE == null || ClientQuestFile.INSTANCE.selfTeamData == null) {
            return;
        }
        if (--wait > 0) return;
        if (chapters == null) {
            chapters = new ArrayList<>(ClientQuestFile.INSTANCE.getAllChapters());
            QuestClustersEditor.LOGGER.info("[tour] {} chapters, editor data for {} chapters / {} quests",
                    chapters.size(), ClientEditor.book().chapters.size(), ClientEditor.book().quests.size());
        } else if (index >= 0) {
            Chapter c = chapters.get(index);
            int special = 0;
            for (ChapterImage img : c.getImages()) {
                if (CanvasElement.parse(img.getClickAction().actionData()) != null) special++;
            }
            var ce = ClientEditor.book().chapter(c.id);
            QuestClustersEditor.LOGGER.info("[tour] {} '{}': visible={} lines={} drawnSpokes={} specialImages={} background={} prerequisites={}",
                    index, c.getTitle().getString(), c.isVisible(ClientQuestFile.INSTANCE.selfTeamData), ce.decorativeLines.size(),
                    DecorativeLinesWidget.lastDrawn, special, ce.background, ce.prerequisites.size());
            DevSelfTest.moduleShot("tour_" + (index < 10 ? "0" : "") + index);
        }
        index++;
        if (index >= chapters.size()) {
            QuestClustersEditor.LOGGER.info("[tour] done");
            index = Integer.MAX_VALUE;
            mc.stop();
            return;
        }
        if (screen == null || mc.screen == null) {
            screen = ClientQuestFile.openGui();
        }
        screen.open(chapters.get(index), false);
        wait = 40;
    }
}
