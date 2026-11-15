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
}
