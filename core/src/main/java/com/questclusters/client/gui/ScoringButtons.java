package com.questclusters.client.gui;

import com.questclusters.client.ClientScoring;
import com.questclusters.net.ClusterPayloads;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.client.gui.quests.TabButton;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Buttons in the quest book's top-right column. */
public final class ScoringButtons {
    private ScoringButtons() {
    }

    public static void addTo(Panel panel, QuestScreen screen) {
        boolean edit = ClusterWidgets.canEdit();
        if (ClientScoring.anyScoring() || edit) {
            panel.add(new SettlementButton(panel, screen));
        }
        if (ClientScoring.settings().leaderboard()) {
            panel.add(new LeaderboardButton(panel));
        }
    }

    static final class SettlementButton extends TabButton {
        private final QuestScreen screen;

        SettlementButton(Panel panel, QuestScreen screen) {
            super(panel, Component.translatable("quest_clusters.settlement.screen"), Icons.BOOK);
            this.screen = screen;
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            if (button.isRight() && ClusterWidgets.canEdit()) {
                ScoringEditor.openSettlementEditor(screen);
            } else {
                ClientScoring.openSettlement();
            }
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            var r = ClientScoring.myResult();
            list.add(getTitle());
            list.add(ClientScoring.settings().showMax()
                    ? Component.translatable("quest_clusters.settlement.total", r.total(), r.max(), String.format("%.1f", r.percent())).withStyle(ChatFormatting.GRAY)
                    : Component.translatable("quest_clusters.settlement.total_nomax", r.total()).withStyle(ChatFormatting.GRAY));
            if (ClusterWidgets.canEdit()) {
                list.add(Component.translatable("quest_clusters.settlement.edit_hint").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    static final class LeaderboardButton extends TabButton {
        LeaderboardButton(Panel panel) {
            super(panel, Component.translatable("quest_clusters.leaderboard.title"), Icons.MONEY_BAG);
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            PacketDistributor.sendToServer(new ClusterPayloads.RequestLeaderboard());
        }
    }
}
