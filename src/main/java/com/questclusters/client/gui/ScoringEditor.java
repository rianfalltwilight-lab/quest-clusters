package com.questclusters.client.gui;

import com.questclusters.client.ClientScoring;
import com.questclusters.data.ScoreEntry;
import com.questclusters.data.SettlementSettings;
import com.questclusters.net.ClusterPayloads;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Editor UI for scores, nodes and the settlement page. Changes go to the server and are undoable. */
public final class ScoringEditor {
    private ScoringEditor() {
    }

    static void addQuestItems(List<ContextMenuItem> menu, QuestScreen screen, Movable movable) {
        if (movable instanceof Quest q) {
            ScoreEntry e = ClientScoring.entry(q.id);
            Component label = e.isEmpty() ? Component.translatable("quest_clusters.menu.scoring")
                    : Component.translatable("quest_clusters.menu.scoring_set", e.score(), e.isNode() ? "✎" : "");
            menu.add(new ContextMenuItem(label, Icons.ART, b -> openQuestEditor(screen, q)));
        }
    }

    static void addSelectionItems(List<ContextMenuItem> menu, QuestScreen screen, List<Movable> selected) {
        List<Quest> quests = selected.stream().filter(m -> m instanceof Quest).map(m -> (Quest) m).toList();
        if (!quests.isEmpty()) {
            menu.add(new ContextMenuItem(Component.translatable("quest_clusters.menu.scoring_bulk", quests.size()), Icons.ART,
                    b -> openBulkEditor(screen, quests)));
        }
    }

    public static void openQuestEditor(QuestScreen screen, Quest q) {
        ScoreEntry[] e = {ClientScoring.entry(q.id)};
        ConfigGroup g = new ConfigGroup("quest_clusters.scoring", accepted -> {
            screen.openGui();
            if (accepted) PacketDistributor.sendToServer(new ClusterPayloads.SetScore(q.id, e[0]));
        });
        g.addInt("score", e[0].score(), v -> e[0] = new ScoreEntry(v, e[0].node(), e[0].opensSettlement()), 0,
                -ScoreEntry.MAX_SCORE, ScoreEntry.MAX_SCORE).setOrder(0);
        g.addString("node", e[0].node(), v -> e[0] = new ScoreEntry(e[0].score(), v, e[0].opensSettlement()), "").setOrder(1);
        g.addBool("opens_settlement", e[0].opensSettlement(), v -> e[0] = new ScoreEntry(e[0].score(), e[0].node(), v), false).setOrder(2);
        new EditConfigScreen(g).openGui();
    }

    static void openBulkEditor(QuestScreen screen, List<Quest> quests) {
        int[] score = {0};
        ConfigGroup g = new ConfigGroup("quest_clusters.scoring", accepted -> {
            screen.openGui();
            if (accepted) {
                for (Quest q : quests) {
                    ScoreEntry old = ClientScoring.entry(q.id);
                    PacketDistributor.sendToServer(new ClusterPayloads.SetScore(q.id, new ScoreEntry(score[0], old.node(), old.opensSettlement())));
                }
            }
        });
        g.addInt("score", 0, v -> score[0] = v, 0, -ScoreEntry.MAX_SCORE, ScoreEntry.MAX_SCORE);
        new EditConfigScreen(g).openGui();
    }

    public static void openSettlementEditor(QuestScreen screen) {
        SettlementSettings s = ClientScoring.settings();
        String[] title = {s.title()};
        List<String> intro = new ArrayList<>(s.intro()), outro = new ArrayList<>(s.outro()), ranks = new ArrayList<>(s.rankLines());
        boolean[] flags = {s.showMax(), s.leaderboard()};
        ConfigGroup g = new ConfigGroup("quest_clusters.settlement", accepted -> {
            screen.openGui();
            if (accepted) {
                PacketDistributor.sendToServer(new ClusterPayloads.SetSettlement(new SettlementSettings(title[0], intro, outro,
                        SettlementSettings.parseRanks(ranks), flags[0], flags[1])));
            }
        });
        int o = 0;
        g.addString("title", title[0], v -> title[0] = v, SettlementSettings.DEFAULT.title()).setOrder(o++);
        g.addList("intro", intro, new StringConfig(), v -> { intro.clear(); intro.addAll(v); }, "").setOrder(o++);
        g.addList("outro", outro, new StringConfig(), v -> { outro.clear(); outro.addAll(v); }, "").setOrder(o++);
        g.addList("ranks", ranks, new StringConfig(), v -> { ranks.clear(); ranks.addAll(v); }, "0=").setOrder(o++);
        g.addBool("show_max", flags[0], v -> flags[0] = v, true).setOrder(o++);
        g.addBool("leaderboard", flags[1], v -> flags[1] = v, true).setOrder(o++);
        new EditConfigScreen(g).openGui();
    }
}
