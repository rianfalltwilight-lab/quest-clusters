package com.questclusters.mixin.client;

import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Method names are prefixed so they cannot clash with other addons' accessors on the same class. */
@Mixin(value = QuestPanel.class, remap = false)
public interface QuestPanelAccessor {
    @Accessor("questScreen")
    QuestScreen quest_clusters$getQuestScreen();

    @Accessor("questMinX")
    double quest_clusters$getQuestMinX();

    @Accessor("questMinY")
    double quest_clusters$getQuestMinY();

    @Accessor("mouseOverQuest")
    QuestButton quest_clusters$getMouseOverQuest();

    @Accessor("questX")
    void quest_clusters$setQuestX(double x);

    @Accessor("questY")
    void quest_clusters$setQuestY(double y);
}
