package com.questclusters.editor.net;

import com.questclusters.editor.QuestClustersEditor;
import com.questclusters.editor.client.ClientEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Editor payloads. E1: the server sends the whole book's editor data on login and after every change. */
public final class EditorNetwork {
    public static final String PROTOCOL = "1";

    public record SyncBook(CompoundTag data) implements CustomPacketPayload {
        public static final Type<SyncBook> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuestClustersEditor.MOD_ID, "sync_book"));
        public static final StreamCodec<ByteBuf, SyncBook> CODEC = ByteBufCodecs.COMPOUND_TAG.map(SyncBook::new, SyncBook::data);

        @Override
        public Type<SyncBook> type() {
            return TYPE;
        }
    }

    /** Editor → server: apply a change (see EditorPatch). */
    public record Edit(String action, CompoundTag patch) implements CustomPacketPayload {
        public static final Type<Edit> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuestClustersEditor.MOD_ID, "edit"));
        public static final StreamCodec<ByteBuf, Edit> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Edit::action, ByteBufCodecs.COMPOUND_TAG, Edit::patch, Edit::new);

        @Override
        public Type<Edit> type() {
            return TYPE;
        }
    }

    public record PersistentRequest(java.util.List<String> keys) implements CustomPacketPayload {
        public static final Type<PersistentRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuestClustersEditor.MOD_ID, "persistent_request"));
        public static final StreamCodec<ByteBuf, PersistentRequest> CODEC = ByteBufCodecs.stringUtf8(128)
                .apply(ByteBufCodecs.list(64)).map(PersistentRequest::new, PersistentRequest::keys);

        @Override
        public Type<PersistentRequest> type() {
            return TYPE;
        }
    }

    public record PersistentResponse(java.util.List<String> keys, CompoundTag forge, CompoundTag kubejs) implements CustomPacketPayload {
        public static final Type<PersistentResponse> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuestClustersEditor.MOD_ID, "persistent_response"));
        public static final StreamCodec<ByteBuf, PersistentResponse> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(64)), PersistentResponse::keys,
                ByteBufCodecs.COMPOUND_TAG, PersistentResponse::forge, ByteBufCodecs.COMPOUND_TAG, PersistentResponse::kubejs,
                PersistentResponse::new);

        @Override
        public Type<PersistentResponse> type() {
            return TYPE;
        }
    }

    public static void sendPersistent(ServerPlayer p, java.util.List<String> keys, CompoundTag forge, CompoundTag kube) {
        PacketDistributor.sendToPlayer(p, new PersistentResponse(keys, forge, kube));
    }

    private EditorNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar(PROTOCOL);
        r.playToClient(SyncBook.TYPE, SyncBook.CODEC, (p, ctx) -> ClientEditor.replace(p.data()));
        r.playToServer(Edit.TYPE, Edit.CODEC, (p, ctx) -> com.questclusters.editor.server.ServerEditor.handleEdit(
                (ServerPlayer) ctx.player(), p.action(), p.patch()));
        r.playToServer(PersistentRequest.TYPE, PersistentRequest.CODEC,
                (p, ctx) -> com.questclusters.editor.PersistentData.handleRequest((ServerPlayer) ctx.player(), p.keys()));
        r.playToClient(PersistentResponse.TYPE, PersistentResponse.CODEC,
                (p, ctx) -> com.questclusters.editor.client.desc.ClientPersistentData.receive(p.keys(), p.forge(), p.kubejs()));
    }

    public static void send(ServerPlayer player, CompoundTag data) {
        PacketDistributor.sendToPlayer(player, new SyncBook(data));
    }
}
