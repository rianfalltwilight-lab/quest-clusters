package com.questclusters.mixin.client;

import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import dev.ftb.mods.ftbquests.quest.Quest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = QuestButton.class, remap = false)
public interface QuestButtonAccessor {
    /** For a quest link button this is the linked quest. */
    @Accessor("quest")
    Quest quest_clusters$getQuest();
}
