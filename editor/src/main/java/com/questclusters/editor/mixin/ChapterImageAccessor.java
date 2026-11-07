package com.questclusters.editor.mixin;

import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.ImageClickAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ChapterImage.class, remap = false)
public interface ChapterImageAccessor {
    @Accessor("clickAction")
    void quest_clusters_editor$setClickAction(ImageClickAction action);
}
