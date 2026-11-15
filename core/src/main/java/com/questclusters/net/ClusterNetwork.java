package com.questclusters.net;

import com.questclusters.platform.Net;
import com.questclusters.client.ClientClusters;
import com.questclusters.data.QuestCluster;
import com.questclusters.server.ServerClusters;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.questclusters.platform.ModContext;

/**
 * Packet registration and send helpers. Handlers run on the main thread.
 */
public final class ClusterNetwork {
    public static final String PROTOCOL = "1";

    private ClusterNetwork() {
    }

    public static void register(ModContext ctx) {
        Net.Channel r = Net.channel(ctx, PROTOCOL);

        r.toClient(ClusterPayloads.SyncAll.TYPE, ClusterPayloads.SyncAll.CODEC,
                p -> ClientClusters.replaceAll(p.clusters()));
        r.toClient(ClusterPayloads.Upsert.TYPE, ClusterPayloads.Upsert.CODEC,
                p -> ClientClusters.upsert(p.cluster()));
        r.toClient(ClusterPayloads.Remove.TYPE, ClusterPayloads.Remove.CODEC,
                p -> ClientClusters.remove(p.clusterId()));

        r.toClient(ClusterPayloads.ScoringSync.TYPE, ClusterPayloads.ScoringSync.CODEC,
                p -> com.questclusters.client.ClientScoring.set(p.entries(), p.settings()));
        r.toClient(ClusterPayloads.OpenSettlement.TYPE, ClusterPayloads.OpenSettlement.CODEC,
                p -> com.questclusters.client.ClientScoring.openSettlement());
        r.toClient(ClusterPayloads.Leaderboard.TYPE, ClusterPayloads.Leaderboard.CODEC,
                p -> com.questclusters.client.ClientScoring.showLeaderboard(p.rows()));
        r.toClient(ClusterPayloads.LimitsSync.TYPE, ClusterPayloads.LimitsSync.CODEC,
                p -> com.questclusters.client.ClientLimits.set(p.quests(), p.rewards(), p.teamCounts(), p.playerCounts(), p.personal()));
        r.toClient(ClusterPayloads.PersonalSync.TYPE, ClusterPayloads.PersonalSync.CODEC,
                p -> com.questclusters.client.ClientPersonal.set(p.state()));
        r.toServer(ClusterPayloads.SetPersonal.TYPE, ClusterPayloads.SetPersonal.CODEC,
                (p, player) -> com.questclusters.server.ServerLimits.handleSetPersonal(player, p.questId(), p.personal()));
        r.toServer(ClusterPayloads.SetQuestLimit.TYPE, ClusterPayloads.SetQuestLimit.CODEC,
                (p, player) -> com.questclusters.server.ServerLimits.handleSetQuest(player, p.questId(), p.max()));
        r.toServer(ClusterPayloads.SetRewardLimit.TYPE, ClusterPayloads.SetRewardLimit.CODEC,
                (p, player) -> com.questclusters.server.ServerLimits.handleSetReward(player, p.rewardId(), p.limit()));
        r.toServer(ClusterPayloads.SetScore.TYPE, ClusterPayloads.SetScore.CODEC,
                (p, player) -> com.questclusters.server.ServerScoring.handleSetEntry(player, p.questId(), p.entry()));
        r.toServer(ClusterPayloads.SetSettlement.TYPE, ClusterPayloads.SetSettlement.CODEC,
                (p, player) -> com.questclusters.server.ServerScoring.handleSetSettings(player, p.settings()));
        r.toServer(ClusterPayloads.RequestLeaderboard.TYPE, ClusterPayloads.RequestLeaderboard.CODEC,
                (p, player) -> com.questclusters.server.ServerScoring.handleLeaderboard(player));
        r.toServer(ClusterPayloads.Create.TYPE, ClusterPayloads.Create.CODEC,
                (p, player) -> ServerClusters.handleCreate(player, p.chapterId(), p.title(), p.members()));
        r.toServer(ClusterPayloads.Update.TYPE, ClusterPayloads.Update.CODEC,
                (p, player) -> ServerClusters.handleUpdate(player, p.cluster()));
        r.toServer(ClusterPayloads.Paste.TYPE, ClusterPayloads.Paste.CODEC,
                (p, player) -> ServerClusters.handlePaste(player, p.chapterId(), p.clusters()));
        r.toServer(ClusterPayloads.Move.TYPE, ClusterPayloads.Move.CODEC,
                (p, player) -> ServerClusters.handleMove(player, p.clusterId(), p.dx(), p.dy(), p.membersOnly()));
        r.toServer(ClusterPayloads.Dissolve.TYPE, ClusterPayloads.Dissolve.CODEC,
                (p, player) -> ServerClusters.handleDissolve(player, p.clusterId()));
    }

    public static void sendFullSync(ServerPlayer player) {
        Net.sendToPlayer(player, new ClusterPayloads.SyncAll(ServerClusters.store().all().stream().toList()));
    }

    public static void sendFullSyncToAll(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(ClusterNetwork::sendFullSync);
    }

    public static void sendUpsertToAll(MinecraftServer server, QuestCluster cluster) {
        Net.sendToAll(new ClusterPayloads.Upsert(cluster));
    }

    public static void sendRemoveToAll(MinecraftServer server, long clusterId) {
        Net.sendToAll(new ClusterPayloads.Remove(clusterId));
    }
}
