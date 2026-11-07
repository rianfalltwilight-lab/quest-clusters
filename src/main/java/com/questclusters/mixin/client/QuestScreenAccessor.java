package com.questclusters.mixin.client;

import dev.ftb.mods.ftbquests.client.gui.quests.ChapterPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = QuestScreen.class, remap = false)
public interface QuestScreenAccessor {
    @Accessor("selectedChapter")
    Chapter quest_clusters$getSelectedChapter();

    @Accessor("movingObjects")
    boolean quest_clusters$isMovingObjects();

    @Accessor("selectedObjects")
    java.util.List<dev.ftb.mods.ftbquests.quest.Movable> quest_clusters$getSelectedObjects();

    @Accessor("chapterPanel")
    ChapterPanel quest_clusters$getChapterPanel();

    @Invoker("getSnappedXY")
    com.mojang.datafixers.util.Pair<Double, Double> quest_clusters$invokeGetSnappedXY();

    @Invoker("copyObjectsToClipboard")
    boolean quest_clusters$invokeCopy();

    @Invoker("pasteSelectedQuest")
    boolean quest_clusters$invokePaste(boolean withDeps, Chapter chapter);
}
