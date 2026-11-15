package com.questclusters.platform;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Packets of one mod, the same API on every loader. Handlers run on the main thread. A channel is required on both
 * sides: a client or server without the mod (or with another protocol version) is refused.
 */
public final class Net {
    public interface ClientHandler<T> {
        void handle(T payload);
    }

    public interface ServerHandler<T> {
        void handle(T payload, ServerPlayer sender);
    }

    private Net() {
    }

    public static Channel channel(ModContext ctx, String protocol) {
        return new Channel(ctx, protocol);
    }

    public static final class Channel {
        private final List<Consumer<PayloadRegistrar>> registrations = new ArrayList<>();

        private Channel(ModContext ctx, String protocol) {
            ctx.modBus().addListener((RegisterPayloadHandlersEvent e) -> {
                PayloadRegistrar r = e.registrar(protocol);
                registrations.forEach(c -> c.accept(r));
            });
        }

        public <T extends CustomPacketPayload> Channel toClient(CustomPacketPayload.Type<T> type, StreamCodec<ByteBuf, T> codec, ClientHandler<T> handler) {
            registrations.add(r -> r.playToClient(type, codec, (p, ctx) -> handler.handle(p)));
            return this;
        }

        public <T extends CustomPacketPayload> Channel toServer(CustomPacketPayload.Type<T> type, StreamCodec<ByteBuf, T> codec, ServerHandler<T> handler) {
            registrations.add(r -> r.playToServer(type, codec, (p, ctx) -> handler.handle(p, (ServerPlayer) ctx.player())));
            return this;
        }
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToAll(CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
