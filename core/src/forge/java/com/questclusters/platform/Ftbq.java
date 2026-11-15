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
}
