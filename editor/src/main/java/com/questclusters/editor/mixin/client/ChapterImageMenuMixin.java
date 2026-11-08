package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.gui.EditorMenus;
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

/** Chapter images build their own menu (not through addObjectMenuItems). */
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
    private List<ContextMenuItem> quest_clusters_editor$imageItems(List<ContextMenuItem> menu) {
        List<ContextMenuItem> out = new ArrayList<>(menu);
        EditorMenus.imageItems(out, questScreen, chapterImage);
        return out;
    }
}
