package com.questclusters.server;

import java.util.UUID;

/** Added to FTB Quests' TeamData: whose personal state is in it right now, and the team's own state to restore. */
public interface PersonalSlot {
    UUID quest_clusters$loaded();

    void quest_clusters$setLoaded(UUID player);

    PersonalState quest_clusters$neutral();

    void quest_clusters$setNeutral(PersonalState state);
}
