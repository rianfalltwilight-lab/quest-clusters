package com.questclusters.net;

import com.questclusters.client.ClientClusters;
import com.questclusters.data.QuestCluster;
import com.questclusters.server.ServerClusters;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Packet registration and send helpers. Handlers run on the main thread (NeoForge default).
 */
public final class ClusterNetwork {
    public static final String PROTOCOL = "1";

    private ClusterNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(PROTOCOL);

        r.playToClient(ClusterPayloads.SyncAll.TYPE, ClusterPayloads.SyncAll.CODEC,
                (p, ctx) -> ClientClusters.replaceAll(p.clusters()));
        r.playToClient(ClusterPayloads.Upsert.TYPE, ClusterPayloads.Upsert.CODEC,
                (p, ctx) -> ClientClusters.upsert(p.cluster()));
        r.playToClient(ClusterPayloads.Remove.TYPE, ClusterPayloads.Remove.CODEC,
                (p, ctx) -> ClientClusters.remove(p.clusterId()));

        r.playToServer(ClusterPayloads.Create.TYPE, ClusterPayloads.Create.CODEC,
                (p, ctx) -> ServerClusters.handleCreate((ServerPlayer) ctx.player(), p.chapterId(), p.title(), p.members()));
        r.playToServer(ClusterPayloads.Update.TYPE, ClusterPayloads.Update.CODEC,
                (p, ctx) -> ServerClusters.handleUpdate((ServerPlayer) ctx.player(), p.cluster()));
        r.playToServer(ClusterPayloads.Paste.TYPE, ClusterPayloads.Paste.CODEC,
                (p, ctx) -> ServerClusters.handlePaste((ServerPlayer) ctx.player(), p.chapterId(), p.clusters()));
        r.playToServer(ClusterPayloads.Dissolve.TYPE, ClusterPayloads.Dissolve.CODEC,
                (p, ctx) -> ServerClusters.handleDissolve((ServerPlayer) ctx.player(), p.clusterId()));
    }

    public static void sendFullSync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ClusterPayloads.SyncAll(ServerClusters.store().all().stream().toList()));
    }

    public static void sendFullSyncToAll(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(ClusterNetwork::sendFullSync);
    }

    public static void sendUpsertToAll(MinecraftServer server, QuestCluster cluster) {
        PacketDistributor.sendToAllPlayers(new ClusterPayloads.Upsert(cluster));
    }

    public static void sendRemoveToAll(MinecraftServer server, long clusterId) {
        PacketDistributor.sendToAllPlayers(new ClusterPayloads.Remove(clusterId));
    }
}
