package com.questclusters.client;

import com.questclusters.data.ClusterTemplate;
import com.questclusters.data.QuestCluster;
import com.questclusters.net.ClusterPayloads;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Lets clusters travel with Quest Enhance's multi-select copy/paste (FTB Quests alone can only copy one object).
 * On copy we remember every cluster whose members are all selected; on paste we ask the server to rebuild them
 * around the new copies, one tick later so the server has already processed the copy requests.
 */
public final class ClusterClipboard {
    private record Copied(double anchorX, double anchorY, List<QuestCluster> clusters, String[] clipboard) {
    }

    private static Copied copied;
    private static boolean captureClipboard;

    private ClusterClipboard() {
    }

    private static boolean multiCopyAvailable() {
        return ModList.get().isLoaded("quest_enhance");
    }

    /** Called before the screen copies its selection. */
    public static void onCopy(List<Movable> selection, Chapter chapter) {
        copied = null;
        if (!multiCopyAvailable() || chapter == null || selection.size() < 2) {
            return;
        }
        Set<Long> ids = new HashSet<>();
        double ax = Double.MAX_VALUE, ay = Double.MAX_VALUE;
        for (Movable m : selection) {
            ids.add(m.getMovableID());
            ax = Math.min(ax, m.getX());
            ay = Math.min(ay, m.getY());
        }
        List<QuestCluster> whole = new ArrayList<>();
        for (QuestCluster c : ClientClusters.store().byChapter(chapter.id)) {
            if (ids.containsAll(c.members()) && c.members().stream().allMatch(ClusterClipboard::copyable)) {
                whole.add(c);
            }
        }
        if (!whole.isEmpty()) {
            copied = new Copied(ax, ay, whole, new String[1]);
            captureClipboard = true; // the copy itself writes the clipboard after we return
        }
    }

    /** Quest Enhance copies quests and chapter images only. */
    private static boolean copyable(long id) {
        var o = ClientQuestFile.INSTANCE.getBase(id);
        return o instanceof Quest || o instanceof ChapterImage;
    }

    /** Called before the screen pastes; (tx, ty) is the snapped paste anchor. */
    public static void onPaste(Chapter target, double tx, double ty) {
        if (copied == null || target == null || copied.clipboard()[0] == null
                || !Objects.equals(Widget.getClipboardString(), copied.clipboard()[0])) {
            return;
        }
        var file = ClientQuestFile.INSTANCE;
        List<ClusterTemplate> templates = new ArrayList<>();
        for (QuestCluster c : copied.clusters()) {
            List<ClusterTemplate.MemberSpec> specs = new ArrayList<>();
            for (long id : c.members()) {
                if (file.getBase(id) instanceof Movable m) {
                    specs.add(new ClusterTemplate.MemberSpec(id, tx + (m.getX() - copied.anchorX()), ty + (m.getY() - copied.anchorY())));
                }
            }
            templates.add(new ClusterTemplate(c.title(), c.subtitle(), c.color(), c.defaultExpanded(),
                    tx + (c.x() - copied.anchorX()), ty + (c.y() - copied.anchorY()), c.width(), c.height(), specs));
        }
        var payload = new ClusterPayloads.Paste(target.id, templates);
        // after this tick's copy requests have gone out
        Minecraft.getInstance().tell(() -> PacketDistributor.sendToServer(payload));
    }

    static void tick() {
        if (captureClipboard && copied != null) {
            captureClipboard = false;
            copied.clipboard()[0] = Widget.getClipboardString();
        }
    }
}
