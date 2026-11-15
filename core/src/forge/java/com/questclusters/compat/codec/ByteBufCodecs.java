package com.questclusters.compat.codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Backport of the codecs from Minecraft 1.21's {@code net.minecraft.network.codec.ByteBufCodecs} the mods use. */
public final class ByteBufCodecs {
    private ByteBufCodecs() {
    }

    private static <V> StreamCodec<ByteBuf, V> of(Function<FriendlyByteBuf, V> read, BiConsumer<FriendlyByteBuf, V> write) {
        return new StreamCodec<>() {
            @Override
            public V decode(ByteBuf buf) {
                return read.apply(wrap(buf));
            }

            @Override
            public void encode(ByteBuf buf, V value) {
                write.accept(wrap(buf), value);
            }
        };
    }

    private static FriendlyByteBuf wrap(ByteBuf buf) {
        return buf instanceof FriendlyByteBuf f ? f : new FriendlyByteBuf(buf);
    }

    public static final StreamCodec<ByteBuf, Boolean> BOOL = of(FriendlyByteBuf::readBoolean, FriendlyByteBuf::writeBoolean);
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = of(FriendlyByteBuf::readVarInt, FriendlyByteBuf::writeVarInt);
    public static final StreamCodec<ByteBuf, Long> VAR_LONG = of(FriendlyByteBuf::readVarLong, FriendlyByteBuf::writeVarLong);
    public static final StreamCodec<ByteBuf, Double> DOUBLE = of(FriendlyByteBuf::readDouble, FriendlyByteBuf::writeDouble);
    public static final StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG = of(b -> {
        CompoundTag t = b.readNbt();
        return t == null ? new CompoundTag() : t;
    }, FriendlyByteBuf::writeNbt);

    public static StreamCodec<ByteBuf, String> stringUtf8(int maxLength) {
        return of(b -> b.readUtf(maxLength), (b, s) -> b.writeUtf(s, maxLength));
    }

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
        return list(Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int maxSize) {
        return element -> new StreamCodec<>() {
            @Override
            public List<V> decode(B buf) {
                int n = wrap(buf).readVarInt();
                if (n > maxSize) throw new io.netty.handler.codec.DecoderException(n + " elements exceed the limit of " + maxSize);
                List<V> out = new ArrayList<>(Math.min(n, 65536));
                for (int i = 0; i < n; i++) out.add(element.decode(buf));
                return out;
            }

            @Override
            public void encode(B buf, List<V> list) {
                if (list.size() > maxSize) throw new io.netty.handler.codec.EncoderException(list.size() + " elements exceed the limit of " + maxSize);
                wrap(buf).writeVarInt(list.size());
                for (V v : list) element.encode(buf, v);
            }
        };
    }
}
