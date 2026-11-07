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

    private EditorNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL).playToClient(SyncBook.TYPE, SyncBook.CODEC, (p, ctx) -> ClientEditor.replace(p.data()));
    }

    public static void send(ServerPlayer player, CompoundTag data) {
        PacketDistributor.sendToPlayer(player, new SyncBook(data));
    }
}
