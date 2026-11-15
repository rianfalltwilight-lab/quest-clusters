package com.questclusters.client.gui;

import com.questclusters.client.ClientLimits;
import com.questclusters.data.ClaimLimits;
import com.questclusters.net.ClusterPayloads;
import com.questclusters.platform.Net;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Editor entries for completion limits (repeatable quests) and claim limits (rewards). */
public final class LimitsEditor {
    private LimitsEditor() {
    }

    public static void addMenuItems(List<ContextMenuItem> menu, QuestScreen screen, QuestObjectBase object) {
        if (!ClusterWidgets.canEdit()) return;
        if (object instanceof Quest q) {
            menu.add(new ContextMenuItem(Component.translatable("quest_clusters.limits.menu.quest"), Icons.TIME,
                    b -> openQuest(screen, q)));
            boolean personal = ClientLimits.limits().isPersonal(q.id);
            menu.add(new ContextMenuItem(Component.translatable(personal ? "quest_clusters.personal.menu.off" : "quest_clusters.personal.menu.on"),
                    Icons.PLAYER, b -> Net.sendToServer(new ClusterPayloads.SetPersonal(q.id, !personal))));
        } else if (object instanceof Reward r) {
            menu.add(new ContextMenuItem(Component.translatable("quest_clusters.limits.menu.reward"), Icons.MONEY_BAG,
                    b -> openReward(screen, r)));
        }
    }

    public static void openQuest(QuestScreen screen, Quest q) {
        int[] max = {ClientLimits.limits().questMax(q.id)};
        ConfigGroup g = new ConfigGroup("quest_clusters.limits", accepted -> {
            screen.openGui();
            if (accepted) Net.sendToServer(new ClusterPayloads.SetQuestLimit(q.id, max[0]));
        });
        g.addInt("max_completions", max[0], v -> max[0] = v, 0, 0, ClaimLimits.MAX);
        new EditConfigScreen(g).openGui();
    }

    public static void openReward(QuestScreen screen, Reward r) {
        ClaimLimits.RewardLimit l = ClientLimits.limits().reward(r.id);
        int[] v = {l.team(), l.player()};
        ConfigGroup g = new ConfigGroup("quest_clusters.limits", accepted -> {
            screen.openGui();
            if (accepted) Net.sendToServer(new ClusterPayloads.SetRewardLimit(r.id, new ClaimLimits.RewardLimit(v[0], v[1])));
        });
        g.addInt("team_claims", v[0], x -> v[0] = x, 0, 0, ClaimLimits.MAX).setOrder(0);
        g.addInt("player_claims", v[1], x -> v[1] = x, 0, 0, ClaimLimits.MAX).setOrder(1);
        new EditConfigScreen(g).openGui();
    }
}
