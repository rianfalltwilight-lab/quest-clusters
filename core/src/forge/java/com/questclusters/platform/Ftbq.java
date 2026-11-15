package com.questclusters.platform;

import com.questclusters.compat.image.ImageId;
import dev.ftb.mods.ftbquests.integration.PermissionsHelper;
import dev.ftb.mods.ftbquests.net.DeleteObjectMessage;
import dev.ftb.mods.ftbquests.net.EditObjectMessage;
import dev.ftb.mods.ftbquests.net.MoveMovableMessage;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Optional;

/** FTB Quests calls whose form differs between its 1.21.1 (2101) and 1.20.1 (2001) versions. */
public final class Ftbq {
    private Ftbq() {
    }

    public static Optional<ServerQuestFile> server() {
        return Optional.ofNullable(ServerQuestFile.INSTANCE);
    }

    /** May edit the quest book right now (editor permission and edit mode). */
    public static boolean canEdit(ServerPlayer player) {
        return ServerQuestFile.INSTANCE != null && ServerQuestFile.INSTANCE.getOrCreateTeamData(player).getCanEdit(player);
    }

    /** Has the editor permission (edit mode may be off). */
    public static boolean isEditor(ServerPlayer player) {
        return PermissionsHelper.hasEditorPermission(player, false);
    }

    public static void setEditMode(ServerQuestFile file, ServerPlayer player, boolean on) {
        file.getOrCreateTeamData(player).setCanEdit(player, on);
    }

    // ------------------------------------------------------------------ client → server requests

    public static void delete(Collection<Long> ids) {
        for (long id : ids) new DeleteObjectMessage(id).sendToServer();
    }

    public static void sendEdit(QuestObjectBase o) {
        new EditObjectMessage(o).sendToServer();
    }

    public static void requestMove(Movable m, long chapterId, double x, double y) {
        new MoveMovableMessage(m, chapterId, x, y).sendToServer();
    }

    // ------------------------------------------------------------------ objects

    /** Quest, quest link or chapter image with this id. */
    public static Movable movable(BaseQuestFile file, long id) {
        if (file.getBase(id) instanceof Movable m) return m;
        return image(file, id);
    }

    /** Chapter images are not quest objects on 1.20.1: found by the id the mixin gives them. */
    public static ChapterImage image(BaseQuestFile file, long id) {
        if (id == 0L) return null;
        ChapterImage[] found = {null};
        file.forAllChapters(ch -> {
            if (found[0] != null) return;
            for (ChapterImage img : ch.getImages()) {
                if (id(img) == id) {
                    found[0] = img;
                    return;
                }
            }
        });
        return found[0];
    }

    public static long id(ChapterImage img) {
        return ((ImageId) (Object) img).quest_clusters$id();
    }

    /** The image's click text (Quest Enhance stores its canvas elements there). */
    public static String clickData(ChapterImage img) {
        return img.getClick();
    }

    /** The chapter's file name, e.g. "getting_started.snbt" (empty before it was saved). */
    public static Optional<String> chapterFileName(dev.ftb.mods.ftbquests.quest.Chapter chapter) {
        return chapter.getPath().map(p -> p.substring(p.lastIndexOf('/') + 1));
    }

    /** Edits the quest's description lines and sends the quest to the server (1.20.1 keeps them on the quest). */
    public static void editDescription(dev.ftb.mods.ftbquests.quest.Quest quest, java.util.function.Consumer<java.util.List<String>> edit) {
        edit.accept(quest.getRawDescription());
        quest.clearCachedData();
        sendEdit(quest);
    }

    /** FTB Quests for 1.20.1 has one language per book: nothing can be missing. */
    public static boolean missingTitleTranslation(BaseQuestFile file, QuestObjectBase o) {
        return false;
    }

    // ------------------------------------------------------------------ editing objects from the client

    public static void write(QuestObjectBase o, net.minecraft.nbt.CompoundTag tag) {
        o.writeData(tag);
    }

    public static void read(QuestObjectBase o, net.minecraft.nbt.CompoundTag tag) {
        o.readData(tag);
    }

    /** Asks the server to create a new object (a task, ...). */
    public static void create(QuestObjectBase o) {
        new dev.ftb.mods.ftbquests.net.CreateObjectMessage(o, null).sendToServer();
    }

    /** A new chapter image from saved data, and Quest Enhance element data for its click (null: none). */
    public static ChapterImage newImage(dev.ftb.mods.ftbquests.quest.Chapter chapter, net.minecraft.nbt.CompoundTag data, String element) {
        ChapterImage img = new ChapterImage(chapter);
        if (element != null) data.putString("click", element);
        img.readData(data);
        return img;
    }

    /** 1.20.1: images are part of their chapter; adding one sends the chapter. No settings screen opens. */
    public static void createImage(ChapterImage img, boolean openEditor) {
        img.getChapter().getImages().add(img);
        sendEdit(img.getChapter());
    }

    /** Changes an image through its saved data and sends its chapter to the server. */
    public static void editImage(ChapterImage img, java.util.function.Consumer<net.minecraft.nbt.CompoundTag> change) {
        var t = img.writeData(new net.minecraft.nbt.CompoundTag());
        change.accept(t);
        img.readData(t);
        sendEdit(img.getChapter());
    }

    /** Replaces the image's Quest Enhance element data. */
    public static void setElement(ChapterImage img, String element) {
        editImage(img, t -> t.putString("click", element));
    }

    public static void copyQuest(long questId, dev.ftb.mods.ftbquests.quest.Chapter target, double x, double y, boolean withDeps) {
        var quest = target.file.getQuest(questId);
        if (quest != null) new dev.ftb.mods.ftbquests.net.CopyQuestMessage(quest, target, x, y, withDeps).sendToServer();
    }

    public static void copyImage(long imageId, dev.ftb.mods.ftbquests.quest.Chapter target, double x, double y) {
        ChapterImage img = image(target.file, imageId);
        if (img != null) new dev.ftb.mods.ftbquests.net.CopyChapterImageMessage(img, target, x, y).sendToServer();
    }
}
