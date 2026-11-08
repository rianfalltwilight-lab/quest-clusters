package com.questclusters.editor.mixin;

import dev.ftb.mods.ftbquests.quest.QuestObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = QuestObject.class, remap = false)
public interface QuestObjectToastAccessor {
    @Accessor("disableToast")
    boolean quest_clusters_editor$disableToast();

    @Accessor("disableToast")
    void quest_clusters_editor$setDisableToast(boolean v);
}
