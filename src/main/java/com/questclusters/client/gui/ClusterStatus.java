package com.questclusters.client.gui;

import com.questclusters.data.QuestCluster;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.client.Minecraft;

import java.util.UUID;

/**
 * Progress of the quests in a cluster, as the local player sees it. Only quests the player can see are counted,
 * so the box never reveals how many hidden quests exist. Quest links are not counted (their quest lives elsewhere).
 */
public record ClusterStatus(int visible, int completed, boolean claimable, boolean startable, boolean anyHidden) {
    public static final ClusterStatus EMPTY = new ClusterStatus(0, 0, false, false, false);

    public boolean allComplete() {
        return visible > 0 && completed == visible;
    }

    public static ClusterStatus of(QuestCluster cluster) {
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        TeamData td = file == null ? null : file.selfTeamData;
        if (td == null || Minecraft.getInstance().player == null) {
            return EMPTY;
        }
        UUID player = Minecraft.getInstance().player.getUUID();
        int visible = 0, completed = 0;
        boolean claimable = false, startable = false, anyHidden = false;
        for (long id : cluster.members()) {
            if (file.getBase(id) instanceof Quest q) {
                if (!q.isVisible(td)) {
                    anyHidden = true;
                    continue;
                }
                visible++;
                if (td.isCompleted(q)) {
                    completed++;
                    if (td.hasUnclaimedRewards(player, q)) {
                        claimable = true;
                    }
                } else if (td.canStartTasks(q)) {
                    startable = true;
                }
            }
        }
        return new ClusterStatus(visible, completed, claimable, startable, anyHidden);
    }
}
