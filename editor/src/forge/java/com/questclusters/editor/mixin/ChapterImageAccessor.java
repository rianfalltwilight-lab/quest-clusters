package com.questclusters.editor.mixin;

import dev.ftb.mods.ftbquests.quest.ChapterImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.20.1: a chapter image's click is a plain string. */
@Mixin(value = ChapterImage.class, remap = false)
public interface ChapterImageAccessor {
    @Accessor("click")
    void quest_clusters_editor$setClick(String click);
}
