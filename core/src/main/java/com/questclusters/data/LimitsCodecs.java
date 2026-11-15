package com.questclusters.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LimitsCodecs {
    private LimitsCodecs() {
    }

    public static final StreamCodec<ByteBuf, ClaimLimits.RewardLimit> REWARD = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ClaimLimits.RewardLimit::team,
            ByteBufCodecs.VAR_INT, ClaimLimits.RewardLimit::player,
            ClaimLimits.RewardLimit::new);

    public static final StreamCodec<ByteBuf, Map<Long, Integer>> COUNTS = byId(ByteBufCodecs.VAR_INT);
    public static final StreamCodec<ByteBuf, Map<Long, ClaimLimits.RewardLimit>> REWARDS = byId(REWARD);

    /** A map keyed by quest object id. */
    public static <V> StreamCodec<ByteBuf, Map<Long, V>> byId(StreamCodec<ByteBuf, V> value) {
        return new StreamCodec<>() {
            @Override
            public Map<Long, V> decode(ByteBuf buf) {
                int n = ByteBufCodecs.VAR_INT.decode(buf);
                if (n < 0 || n > 100_000) throw new IllegalArgumentException("too many entries: " + n);
                Map<Long, V> m = new LinkedHashMap<>();
                for (int i = 0; i < n; i++) m.put(buf.readLong(), value.decode(buf));
                return m;
            }

            @Override
            public void encode(ByteBuf buf, Map<Long, V> m) {
                ByteBufCodecs.VAR_INT.encode(buf, m.size());
                m.forEach((id, v) -> {
                    buf.writeLong(id);
                    value.encode(buf, v);
                });
            }
        };
    }
}
