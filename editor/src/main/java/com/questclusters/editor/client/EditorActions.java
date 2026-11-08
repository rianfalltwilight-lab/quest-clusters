package com.questclusters.editor.client;

import com.questclusters.editor.data.BookExtras;
import com.questclusters.editor.data.ChapterExtras;
import com.questclusters.editor.data.EditorPatch;
import com.questclusters.editor.data.QuestExtras;
import com.questclusters.editor.net.EditorNetwork;
import dev.ftb.mods.ftblibrary.config.ImageResourceConfig;
import dev.ftb.mods.ftblibrary.config.ui.resource.SelectImageResourceScreen;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/** Builds editor changes and sends them; the server validates, records an undo step and syncs everyone. */
public final class EditorActions {
    private EditorActions() {
    }

    /**
     * Applied to the local copy at once (so a second edit sent before the server answers builds on the first), then
     * sent; the server's sync replaces the local copy with the authoritative result (or reverts a rejected edit).
     */
    public static void send(String action, EditorPatch patch) {
        if (!patch.isEmpty()) {
            PacketDistributor.sendToServer(new EditorNetwork.Edit(action, patch.write()));
            patch.applyTo(ClientEditor.book());
        }
    }

    public static void editChapter(String action, long chapterId, Consumer<ChapterExtras> edit) {
        ChapterExtras c = ClientEditor.book().chapter(chapterId).copy();
        edit.accept(c);
        EditorPatch p = new EditorPatch();
        p.chapters.put(chapterId, c);
        send(action, p);
    }

    public static void editQuests(String action, Collection<Long> ids, UnaryOperator<QuestExtras> edit) {
        BookExtras b = ClientEditor.book();
        EditorPatch p = new EditorPatch();
        for (long id : ids) p.quests.put(id, edit.apply(b.quest(id)));
        send(action, p);
    }

    public static void setSidebar(String location) {
        EditorPatch p = new EditorPatch();
        p.sidebar = location;
        send("sidebar_background", p);
    }

    public static void setExcluded(Collection<Long> imageIds, boolean excluded) {
        EditorPatch p = new EditorPatch();
        imageIds.forEach(id -> p.images.put(id, excluded));
        send("box_select", p);
    }

    /** FTB Library's image picker (the one chapter images use); "none" clears. Returns to the quest screen. */
    public static void pickImage(QuestScreen screen, String current, Consumer<String> onPicked) {
        pickImage(screen, current, onPicked, () -> reopen(screen));
    }

    /** As above, but {@code back} reopens whatever screen the picker was opened from (before {@code onPicked}). */
    public static void pickImage(QuestScreen screen, String current, Consumer<String> onPicked, Runnable back) {
        ImageResourceConfig cfg = new ImageResourceConfig();
        cfg.withAllowEmpty(true);
        ResourceLocation rl = current.isEmpty() ? null : ResourceLocation.tryParse(current);
        cfg.setValue(rl == null ? ImageResourceConfig.NONE : rl);
        new SelectImageResourceScreen(cfg, accepted -> {
            back.run();
            if (accepted) {
                ResourceLocation v = cfg.getValue();
                onPicked.accept(v == null || v.equals(ImageResourceConfig.NONE) ? "" : v.toString());
            }
        }).openGui();
    }

    public static void reopen(QuestScreen screen) {
        if (screen != null) screen.openGui();
        else ClientQuestFile.openGui();
    }
}
