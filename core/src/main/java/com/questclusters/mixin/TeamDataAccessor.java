package com.questclusters.mixin;

import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.util.QuestKey;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** FTB Quests' progress maps, for swapping personal quest state in and out. */
@Mixin(value = TeamData.class, remap = false)
public interface TeamDataAccessor {
    @Accessor("taskProgress")
    Long2LongMap quest_clusters$taskProgress();

    @Accessor("started")
    Long2LongMap quest_clusters$started();

    @Accessor("completed")
    Long2LongMap quest_clusters$completed();

    @Accessor("completionCount")
    Long2IntMap quest_clusters$completionCount();

    @Accessor("questRepeatableTime")
    Long2LongMap quest_clusters$repeatTime();

    @Accessor("claimedRewards")
    Object2LongMap<QuestKey> quest_clusters$claimed();
}
