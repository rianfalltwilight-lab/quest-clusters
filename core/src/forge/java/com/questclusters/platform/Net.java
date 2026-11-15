package com.questclusters.platform;

import com.questclusters.compat.codec.StreamCodec;
import com.questclusters.compat.net.CustomPacketPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Packets of one mod, the same API on every loader. Handlers run on the main thread. A channel is required on both
 * sides: a client or server without the mod (or with another protocol version) is refused.
 * <p>
 * Forge 47: each mod has one SimpleChannel ({@code <modid>:main}) carrying one message, an envelope with the
 * payload's id and its encoded body; the id picks the codec and handler.
 */
public final class Net {
    public interface ClientHandler<T> {
        void handle(T payload);
    }

    public interface ServerHandler<T> {
        void handle(T payload, ServerPlayer sender);
    }

    private record Entry(StreamCodec<ByteBuf, Object> codec, ClientHandler<Object> client, ServerHandler<Object> server) {
    }

    private record Envelope(ResourceLocation id, Object payload) {
    }

    private static final Map<ResourceLocation, Entry> ENTRIES = new HashMap<>();
    private static final Map<String, SimpleChannel> CHANNELS = new HashMap<>();

    private Net() {
    }

    public static Channel channel(ModContext ctx, String protocol) {
        SimpleChannel ch = NetworkRegistry.newSimpleChannel(Rl.of(ctx.modId(), "main"), () -> protocol, protocol::equals, protocol::equals);
        ch.messageBuilder(Envelope.class, 0)
                .encoder(Net::encode)
                .decoder(Net::decode)
                .consumerMainThread(Net::handle)
                .add();
        CHANNELS.put(ctx.modId(), ch);
        return new Channel();
    }

    public static final class Channel {
        private Channel() {
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public <T extends CustomPacketPayload> Channel toClient(CustomPacketPayload.Type<T> type, StreamCodec<ByteBuf, T> codec, ClientHandler<T> handler) {
            ENTRIES.put(type.id(), new Entry((StreamCodec) codec, (ClientHandler) handler, null));
            return this;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public <T extends CustomPacketPayload> Channel toServer(CustomPacketPayload.Type<T> type, StreamCodec<ByteBuf, T> codec, ServerHandler<T> handler) {
            ENTRIES.put(type.id(), new Entry((StreamCodec) codec, null, (ServerHandler) handler));
            return this;
        }
    }

    private static void encode(Envelope e, FriendlyByteBuf buf) {
        buf.writeResourceLocation(e.id());
        ENTRIES.get(e.id()).codec().encode(buf, e.payload());
    }

    private static Envelope decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        Entry entry = ENTRIES.get(id);
        if (entry == null) throw new IllegalStateException("Unknown Quest Clusters packet " + id);
        return new Envelope(id, entry.codec().decode(buf));
    }

    private static void handle(Envelope e, Supplier<NetworkEvent.Context> ctx) {
        Entry entry = ENTRIES.get(e.id());
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            if (entry.client() != null) entry.client().handle(e.payload());
        } else if (entry.server() != null && ctx.get().getSender() != null) {
            entry.server().handle(e.payload(), ctx.get().getSender());
        }
    }

    private static SimpleChannel channelOf(CustomPacketPayload payload) {
        return CHANNELS.get(payload.type().id().getNamespace());
    }

    private static Envelope envelope(CustomPacketPayload payload) {
        return new Envelope(payload.type().id(), payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        channelOf(payload).send(PacketDistributor.PLAYER.with(() -> player), envelope(payload));
    }

    public static void sendToAll(CustomPacketPayload payload) {
        channelOf(payload).send(PacketDistributor.ALL.noArg(), envelope(payload));
    }

    public static void sendToServer(CustomPacketPayload payload) {
        channelOf(payload).sendToServer(envelope(payload));
    }
}
