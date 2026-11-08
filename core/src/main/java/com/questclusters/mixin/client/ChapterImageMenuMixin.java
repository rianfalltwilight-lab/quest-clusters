package com.questclusters.mixin.client;

import com.questclusters.client.gui.ClusterEditor;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftbquests.client.gui.quests.ChapterImageButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;

/**
 * Chapter images (pictures, guide points, canvas text, GIFs) build their own context menu instead of going through
 * QuestScreen#addObjectMenuItems: add the cluster entries here too (remove from / add to / create; with a
 * multi-selection that includes the image, the selection entries).
 */
@Mixin(value = ChapterImageButton.class, remap = false)
public abstract class ChapterImageMenuMixin {
    @Shadow
    @Final
    private ChapterImage chapterImage;

    @Shadow
    @Final
    private QuestScreen questScreen;

    @ModifyArg(method = "onClicked", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftblibrary/ui/BaseScreen;openContextMenu(Ljava/util/List;)Ldev/ftb/mods/ftblibrary/ui/ContextMenu;"))
    private List<ContextMenuItem> quest_clusters$clusterItems(List<ContextMenuItem> menu) {
        List<ContextMenuItem> out = new ArrayList<>(menu);
        var selected = ((QuestScreenAccessor) questScreen).quest_clusters$getSelectedObjects();
        if (selected.size() >= 2 && selected.contains(chapterImage)) {
            ClusterEditor.addSelectionMenuItems(out, questScreen, List.copyOf(selected), chapterImage.getChapter());
        } else {
            ClusterEditor.addObjectMenuItems(out, questScreen, chapterImage);
        }
        return out;
    }
}
