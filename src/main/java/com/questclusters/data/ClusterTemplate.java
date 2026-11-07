package com.questclusters.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * A cluster to rebuild after a paste: its look, the target box position, and for each member the object it was
 * copied from plus the exact position the copy was placed at. The server finds the new copies by that position.
 */
public record ClusterTemplate(String title, String subtitle, int color, boolean defaultExpanded,
                              double x, double y, double width, double height, List<MemberSpec> members,
                              ClusterStyle style) {

    public record MemberSpec(long sourceId, double x, double y) {
    }

    public static final StreamCodec<ByteBuf, ClusterTemplate> CODEC = new StreamCodec<>() {
        @Override
        public ClusterTemplate decode(ByteBuf buf) {
            String title = ByteBufCodecs.stringUtf8(QuestCluster.MAX_TITLE_LENGTH * 4).decode(buf);
            String subtitle = ByteBufCodecs.stringUtf8(QuestCluster.MAX_SUBTITLE_LENGTH * 4).decode(buf);
            int color = buf.readInt();
            boolean exp = buf.readBoolean();
            double x = buf.readDouble(), y = buf.readDouble(), w = buf.readDouble(), h = buf.readDouble();
            int n = ByteBufCodecs.VAR_INT.decode(buf);
            if (n < 0 || n > QuestCluster.MAX_MEMBERS) {
                throw new IllegalArgumentException("Too many members in pasted cluster: " + n);
            }
            List<MemberSpec> members = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                members.add(new MemberSpec(buf.readLong(), buf.readDouble(), buf.readDouble()));
            }
            ClusterStyle style = new ClusterStyle(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
            return new ClusterTemplate(title, subtitle, color, exp, x, y, w, h, members, style);
        }

        @Override
        public void encode(ByteBuf buf, ClusterTemplate t) {
            ByteBufCodecs.stringUtf8(QuestCluster.MAX_TITLE_LENGTH * 4).encode(buf, t.title());
            ByteBufCodecs.stringUtf8(QuestCluster.MAX_SUBTITLE_LENGTH * 4).encode(buf, t.subtitle());
            buf.writeInt(t.color());
            buf.writeBoolean(t.defaultExpanded());
            buf.writeDouble(t.x());
            buf.writeDouble(t.y());
            buf.writeDouble(t.width());
            buf.writeDouble(t.height());
            ByteBufCodecs.VAR_INT.encode(buf, t.members().size());
            for (MemberSpec m : t.members()) {
                buf.writeLong(m.sourceId());
                buf.writeDouble(m.x());
                buf.writeDouble(m.y());
            }
            buf.writeInt(t.style().titleColor());
            buf.writeInt(t.style().subtitleColor());
            buf.writeInt(t.style().borderColor());
            buf.writeInt(t.style().frameColor());
        }
    };
}
