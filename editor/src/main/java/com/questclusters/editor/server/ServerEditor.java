package com.questclusters.editor.server;

import com.questclusters.api.QuestBookHooks;
import com.questclusters.editor.QuestClustersEditor;
import com.questclusters.editor.data.BookExtras;
import com.questclusters.editor.data.CanvasElement;
import com.questclusters.editor.data.ChapterExtras;
import com.questclusters.editor.data.ExtrasNbt;
import com.questclusters.editor.data.NodeKeys;
import com.questclusters.editor.net.EditorNetwork;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Server copy of the editor data: loaded with the quest book, pruned and saved with it, synced to players. */
public final class ServerEditor implements QuestBookHooks.Listener {
    public static final ServerEditor INSTANCE = new ServerEditor();
    private static BookExtras book = new BookExtras();
    private static boolean dirty;
    private static CompoundTag syncCache;
    private static int pruneCountdown;

    private ServerEditor() {
    }

    public static BookExtras book() {
        return book;
    }

    /** Replace/mark after a change: saved with the book, re-sent to everybody. */
    public static void changed(ServerQuestFile file) {
        dirty = true;
        syncCache = null;
        file.markDirty();
        if (file.server != null) {
            file.server.getPlayerList().getPlayers().forEach(ServerEditor::send);
        }
    }

    public static Path questsFolder(ServerQuestFile file) {
        return file.getFolder();
    }

    @Override
    public void loaded(ServerQuestFile file) {
        book = EditorStorage.load(questsFolder(file));
        dirty = false;
        syncCache = null;
        if (prune(file, book)) {
            dirty = true;
        }
        QuestClustersEditor.LOGGER.info("Editor data loaded: {} chapters, {} quests", book.chapters.size(), book.quests.size());
        if (file.server != null) {
            file.server.getPlayerList().getPlayers().forEach(ServerEditor::send);
        }
    }

    @Override
    public void saving(ServerQuestFile file) {
        prune(file, book);
        if (!dirty) {
            return;
        }
        EditorStorage.save(questsFolder(file), book, id -> chapterOf(file, id));
        dirty = false;
    }

    @Override
    public void unloaded() {
        book = new BookExtras();
        dirty = false;
        syncCache = null;
    }

    @Override
    public void login(ServerPlayer player) {
        send(player);
    }

    @Override
    public void tick(ServerQuestFile file) {
        if (++pruneCountdown >= 20) { // objects deleted in game: drop their data within a second
            pruneCountdown = 0;
            if (prune(file, book)) {
                changed(file);
            }
        }
    }

    /** Actions with a history label (anything else is shown as a generic edit). */
    private static final java.util.Set<String> ACTIONS = java.util.Set.of("chapter_background", "sidebar_background",
            "quest_background", "view_background", "prerequisites", "decorative_line", "hidden_line", "scroll_paging",
            "box_select", "line_presets", "line_preset");

    /** A change requested by an editor in game: checked, then applied as one undoable step. */
    public static void handleEdit(ServerPlayer player, String action, CompoundTag data) {
        var file = ServerQuestFile.getInstance().orElse(null);
        if (file == null || file.isLoading()) return;
        if (!dev.ftb.mods.ftbquests.integration.PermissionsHelper.canPlayerEdit(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("quest_clusters_editor.error.no_permission")
                    .withStyle(net.minecraft.ChatFormatting.RED), false);
            return;
        }
        com.questclusters.editor.data.EditorPatch patch = com.questclusters.editor.data.EditorPatch.read(data);
        if (patch == null || patch.isEmpty()) {
            QuestClustersEditor.LOGGER.warn("Rejected malformed editor change from {}", player.getGameProfile().getName());
            return;
        }
        // only objects that exist, of the right kind
        patch.chapters.keySet().removeIf(id -> !(file.getBase(id) instanceof Chapter));
        patch.quests.keySet().removeIf(id -> !(file.getBase(id) instanceof Quest));
        patch.images.keySet().removeIf(id -> !(file.getBase(id) instanceof ChapterImage));
        patch.chapters.forEach((id, c) -> c.prerequisites.removeIf(q -> !(file.getBase(q) instanceof Quest quest) || quest.getChapter().id == id));
        if (patch.isEmpty()) return;
        String a = ACTIONS.contains(action) ? action : "edit";
        file.getHistoryStack().addAndApply(file, new EditorChangeEvent(patch, patch.snapshot(book), a));
    }

    static void applyRaw(ServerQuestFile file, com.questclusters.editor.data.EditorPatch patch) {
        patch.applyTo(book);
        prune(file, book);
        changed(file);
    }

    static void send(ServerPlayer p) {
        if (syncCache == null) {
            syncCache = ExtrasNbt.writeAll(book);
        }
        EditorNetwork.send(p, syncCache);
    }

    private static long chapterOf(BaseQuestFile file, long id) {
        var o = file.getBase(id);
        if (o instanceof Quest q) return q.getChapter().id;
        if (o instanceof ChapterImage img) return img.getChapter().id;
        return 0L;
    }

    /** Drops data of objects that no longer exist. Returns true if anything changed. */
    static boolean prune(BaseQuestFile file, BookExtras b) {
        boolean changed = b.chapters.keySet().removeIf(id -> !(file.getBase(id) instanceof Chapter));
        changed |= b.quests.keySet().removeIf(id -> !(file.getBase(id) instanceof Quest));
        changed |= b.excludedImages.removeIf(id -> !(file.getBase(id) instanceof ChapterImage));
        for (var e : b.chapters.entrySet()) {
            Chapter chapter = (Chapter) file.getBase(e.getKey());
            ChapterExtras c = e.getValue();
            changed |= c.prerequisites.removeIf(id -> !(file.getBase(id) instanceof Quest));
            Set<String> nodes = nodesOf(chapter);
            for (int i = c.decorativeLines.size() - 1; i >= 0; i--) {
                List<String> line = c.decorativeLines.get(i);
                changed |= line.removeIf(n -> !nodes.contains(n));
                if (line.size() < 2) {
                    c.decorativeLines.remove(i);
                    changed = true;
                }
            }
            changed |= c.hiddenLines.removeIf(h -> !nodes.contains(h.source()) || !nodes.contains(h.dependency()));
            changed |= c.nodeLinePresets.keySet().removeIf(n -> !nodes.contains(n));
        }
        changed |= b.chapters.values().removeIf(ChapterExtras::isEmpty);
        if (changed) syncCache = null;
        return changed;
    }

    /** Every node key that exists on a chapter's canvas. */
    public static Set<String> nodesOf(Chapter chapter) {
        Set<String> out = new HashSet<>();
        for (Quest q : chapter.getQuests()) out.add(NodeKeys.quest(q.id));
        for (QuestLink l : chapter.getQuestLinks()) out.add(NodeKeys.link(l.id));
        for (ChapterImage img : chapter.getImages()) {
            if (CanvasElement.parse(img.getClickAction().actionData()) instanceof CanvasElement.Anchor a && a.nodeKey() != null) {
                out.add(a.nodeKey());
            }
        }
        return out;
    }
}
