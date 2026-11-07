package com.questclusters.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public final class ClusterCodecs {
    private ClusterCodecs() {
    }

    public static final StreamCodec<ByteBuf, List<Long>> ID_LIST = new StreamCodec<>() {
        @Override
        public List<Long> decode(ByteBuf buf) {
            int n = ByteBufCodecs.VAR_INT.decode(buf);
            if (n < 0 || n > QuestCluster.MAX_MEMBERS) {
                throw new IllegalArgumentException("Too many ids in cluster packet: " + n);
            }
            List<Long> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(buf.readLong());
            }
            return list;
        }

        @Override
        public void encode(ByteBuf buf, List<Long> list) {
            ByteBufCodecs.VAR_INT.encode(buf, list.size());
            for (long id : list) {
                buf.writeLong(id);
            }
        }
    };

    public static final StreamCodec<ByteBuf, QuestCluster> CLUSTER = new StreamCodec<>() {
        @Override
        public QuestCluster decode(ByteBuf buf) {
            return new QuestCluster(
                    buf.readLong(),
                    buf.readLong(),
                    ByteBufCodecs.stringUtf8(QuestCluster.MAX_TITLE_LENGTH * 4).decode(buf),
                    ByteBufCodecs.stringUtf8(QuestCluster.MAX_SUBTITLE_LENGTH * 4).decode(buf),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    ID_LIST.decode(buf),
                    new ClusterStyle(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt())
            );
        }

        @Override
        public void encode(ByteBuf buf, QuestCluster c) {
            buf.writeLong(c.id());
            buf.writeLong(c.chapterId());
            ByteBufCodecs.stringUtf8(QuestCluster.MAX_TITLE_LENGTH * 4).encode(buf, c.title());
            ByteBufCodecs.stringUtf8(QuestCluster.MAX_SUBTITLE_LENGTH * 4).encode(buf, c.subtitle());
            buf.writeInt(c.color());
            buf.writeBoolean(c.defaultExpanded());
            buf.writeDouble(c.x());
            buf.writeDouble(c.y());
            buf.writeDouble(c.width());
            buf.writeDouble(c.height());
            ID_LIST.encode(buf, c.members());
            buf.writeInt(c.style().titleColor());
            buf.writeInt(c.style().subtitleColor());
            buf.writeInt(c.style().borderColor());
            buf.writeInt(c.style().frameColor());
        }
    };

    public static final StreamCodec<ByteBuf, List<QuestCluster>> CLUSTER_LIST =
            CLUSTER.apply(ByteBufCodecs.list());
}
