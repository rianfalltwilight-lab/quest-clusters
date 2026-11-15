package com.questclusters.platform;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbquests.integration.PermissionsHelper;
import dev.ftb.mods.ftbquests.net.DeleteObjectMessage;
import dev.ftb.mods.ftbquests.net.EditObjectMessage;
import dev.ftb.mods.ftbquests.net.MoveMovableMessage;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** FTB Quests calls whose form differs between its 1.21.1 (2101) and 1.20.1 (2001) versions. */
public final class Ftbq {
    private Ftbq() {
    }

    public static Optional<ServerQuestFile> server() {
        return ServerQuestFile.getInstance();
    }

    /** May edit the quest book right now (editor permission and edit mode). */
    public static boolean canEdit(ServerPlayer player) {
        return PermissionsHelper.canPlayerEdit(player);
    }

    /** Has the editor permission (edit mode may be off). */
    public static boolean isEditor(ServerPlayer player) {
        return PermissionsHelper.hasEditorPermission(player);
    }

    public static void setEditMode(ServerQuestFile file, ServerPlayer player, boolean on) {
        file.getOrCreateTeamData(player).setPlayerEditMode(player, on);
    }

    // ------------------------------------------------------------------ client → server requests

    public static void delete(Collection<Long> ids) {
        NetworkManager.sendToServer(new DeleteObjectMessage(List.copyOf(ids)));
    }

    public static void sendEdit(QuestObjectBase o) {
        NetworkManager.sendToServer(EditObjectMessage.forQuestObject(o));
    }

    public static void requestMove(Movable m, long chapterId, double x, double y) {
        NetworkManager.sendToServer(new MoveMovableMessage(m.getMovableID(), chapterId, x, y));
    }

    // ------------------------------------------------------------------ objects

    /** Quest, quest link or chapter image with this id. */
    public static Movable movable(BaseQuestFile file, long id) {
        return file.getBase(id) instanceof Movable m ? m : null;
    }

    public static ChapterImage image(BaseQuestFile file, long id) {
        return file.getBase(id) instanceof ChapterImage img ? img : null;
    }

    public static long id(ChapterImage img) {
        return img.id;
    }

    /** The image's click action data (Quest Enhance stores its canvas elements there). */
    public static String clickData(ChapterImage img) {
        return img.getClickAction().actionData();
    }

    /** Registries for reading item stacks from NBT (none needed on 1.20.1). */
    public static HolderLookup.Provider registries(BaseQuestFile file) {
        return file.holderLookup();
    }

    /** The chapter's file name, e.g. "getting_started.snbt" (empty before it was saved). */
    public static Optional<String> chapterFileName(dev.ftb.mods.ftbquests.quest.Chapter chapter) {
        return chapter.getPath().map(p -> p.getFileName().toString());
    }

    /** Edits the quest's description lines (current language) and saves them through FTB Quests. */
    public static void editDescription(dev.ftb.mods.ftbquests.quest.Quest quest, java.util.function.Consumer<java.util.List<String>> edit) {
        quest.modifyTranslatableListValue(dev.ftb.mods.ftbquests.quest.translation.TranslationKey.QUEST_DESC, edit);
    }

    /** The object's title is missing in the book's current language (falls back to another one). */
    public static boolean missingTitleTranslation(BaseQuestFile file, QuestObjectBase o) {
        return file.getTranslationManager().hasMissingTranslation(o, dev.ftb.mods.ftbquests.quest.translation.TranslationKey.TITLE);
    }

    // ------------------------------------------------------------------ editing objects from the client

    /** Saves an object's data (with registries on 1.21.1). */
    public static void write(QuestObjectBase o, net.minecraft.nbt.CompoundTag tag) {
        o.writeData(tag, o.getQuestFile().holderLookup());
    }

    public static void read(QuestObjectBase o, net.minecraft.nbt.CompoundTag tag) {
        o.readData(tag, o.getQuestFile().holderLookup());
    }

    /** Asks the server to create a new object (a task, ...). */
    public static void create(QuestObjectBase o) {
        NetworkManager.sendToServer(dev.ftb.mods.ftbquests.net.CreateObjectMessage.requestCreation(o));
    }

    /** A new chapter image from saved data, and Quest Enhance element data for its click (null: none). */
    public static ChapterImage newImage(dev.ftb.mods.ftbquests.quest.Chapter chapter, net.minecraft.nbt.CompoundTag data, String element) {
        ChapterImage img = new ChapterImage(0L, chapter);
        if (element != null) data.putString("click_action", "none:" + element);
        img.readData(data, chapter.getQuestFile().holderLookup());
        return img;
    }

    /** Asks the server to add the image; {@code openEditor}: then open its settings. */
    public static void createImage(ChapterImage img, boolean openEditor) {
        NetworkManager.sendToServer(dev.ftb.mods.ftbquests.net.CreateObjectMessage.requestCreation(img, openEditor));
    }

    /** Changes an image through its saved data and sends it to the server. */
    public static void editImage(ChapterImage img, java.util.function.Consumer<net.minecraft.nbt.CompoundTag> change) {
        var t = new net.minecraft.nbt.CompoundTag();
        img.writeData(t, img.getQuestFile().holderLookup());
        change.accept(t);
        img.readData(t, img.getQuestFile().holderLookup());
        sendEdit(img);
    }

    /** Replaces the image's Quest Enhance element data. */
    public static void setElement(ChapterImage img, String element) {
        editImage(img, t -> t.putString("click_action", "none:" + element));
    }

    public static void copyQuest(long questId, dev.ftb.mods.ftbquests.quest.Chapter target, double x, double y, boolean withDeps) {
        NetworkManager.sendToServer(new dev.ftb.mods.ftbquests.net.CopyQuestMessage(questId, target.id, x, y, withDeps));
    }

    public static void copyImage(long imageId, dev.ftb.mods.ftbquests.quest.Chapter target, double x, double y) {
        NetworkManager.sendToServer(new dev.ftb.mods.ftbquests.net.CopyChapterImageMessage(imageId, target.id, x, y));
    }

    /** The quest object or chapter image with this id, or null (chapter images are not quest objects on 1.20.1). */
    public static Object lookup(BaseQuestFile file, long id) {
        Object o = file.getBase(id);
        return o != null ? o : image(file, id);
    }

    /** Sends a player FTB Quests' full data of their team again. */
    public static void syncTeamData(ServerPlayer player, dev.ftb.mods.ftbquests.quest.TeamData td) {
        dev.architectury.networking.NetworkManager.sendToPlayer(player, new dev.ftb.mods.ftbquests.net.SyncTeamDataMessage(td));
    }
}
