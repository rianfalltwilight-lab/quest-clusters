package com.questclusters.net;

import com.questclusters.QuestClusters;
import com.questclusters.data.ClusterCodecs;
import com.questclusters.data.QuestCluster;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** All packets of the mod. S2C = server to client, C2S = client to server. */
public final class ClusterPayloads {
    private ClusterPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> newType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuestClusters.MOD_ID, path));
    }

    // ------------------------------------------------------------------ S2C

    /** Replaces the client's whole cluster set (login, /ftbquests reload). */
    public record SyncAll(List<QuestCluster> clusters) implements CustomPacketPayload {
        public static final Type<SyncAll> TYPE = newType("sync_all");
        public static final StreamCodec<ByteBuf, SyncAll> CODEC =
                ClusterCodecs.CLUSTER_LIST.map(SyncAll::new, SyncAll::clusters);

        @Override
        public Type<SyncAll> type() {
            return TYPE;
        }
    }

    public record Upsert(QuestCluster cluster) implements CustomPacketPayload {
        public static final Type<Upsert> TYPE = newType("upsert");
        public static final StreamCodec<ByteBuf, Upsert> CODEC = ClusterCodecs.CLUSTER.map(Upsert::new, Upsert::cluster);

        @Override
        public Type<Upsert> type() {
            return TYPE;
        }
    }

    public record Remove(long clusterId) implements CustomPacketPayload {
        public static final Type<Remove> TYPE = newType("remove");
        public static final StreamCodec<ByteBuf, Remove> CODEC = ByteBufCodecs.VAR_LONG.map(Remove::new, Remove::clusterId);

        @Override
        public Type<Remove> type() {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------ C2S (editor actions, permission-checked on the server)

    public record Create(long chapterId, String title, List<Long> members) implements CustomPacketPayload {
        public static final Type<Create> TYPE = newType("create");
        public static final StreamCodec<ByteBuf, Create> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Create::chapterId,
                ByteBufCodecs.stringUtf8(QuestCluster.MAX_TITLE_LENGTH * 4), Create::title,
                ClusterCodecs.ID_LIST, Create::members,
                Create::new);

        @Override
        public Type<Create> type() {
            return TYPE;
        }
    }

    public record Update(QuestCluster cluster) implements CustomPacketPayload {
        public static final Type<Update> TYPE = newType("update");
        public static final StreamCodec<ByteBuf, Update> CODEC = ClusterCodecs.CLUSTER.map(Update::new, Update::cluster);

        @Override
        public Type<Update> type() {
            return TYPE;
        }
    }

    public record Dissolve(long clusterId) implements CustomPacketPayload {
        public static final Type<Dissolve> TYPE = newType("dissolve");
        public static final StreamCodec<ByteBuf, Dissolve> CODEC = ByteBufCodecs.VAR_LONG.map(Dissolve::new, Dissolve::clusterId);

        @Override
        public Type<Dissolve> type() {
            return TYPE;
        }
    }

    /** Rebuild clusters around objects that were just pasted (sent one tick after the copy requests). */
    public record Paste(long chapterId, List<com.questclusters.data.ClusterTemplate> clusters) implements CustomPacketPayload {
        public static final Type<Paste> TYPE = newType("paste");
        public static final StreamCodec<ByteBuf, Paste> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Paste::chapterId,
                com.questclusters.data.ClusterTemplate.CODEC.apply(ByteBufCodecs.list(16)), Paste::clusters,
                Paste::new);

        @Override
        public Type<Paste> type() {
            return TYPE;
        }
    }

    /** Move a cluster and all its members by (dx, dy) canvas units, as one undo step. */
    public record Move(long clusterId, double dx, double dy) implements CustomPacketPayload {
        public static final Type<Move> TYPE = newType("move");
        public static final StreamCodec<ByteBuf, Move> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Move::clusterId,
                ByteBufCodecs.DOUBLE, Move::dx,
                ByteBufCodecs.DOUBLE, Move::dy,
                Move::new);

        @Override
        public Type<Move> type() {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------ scoring / settlement / leaderboard

    public record ScoringSync(java.util.Map<Long, com.questclusters.data.ScoreEntry> entries,
                              com.questclusters.data.SettlementSettings settings) implements CustomPacketPayload {
        public static final Type<ScoringSync> TYPE = newType("scoring_sync");
        public static final StreamCodec<ByteBuf, ScoringSync> CODEC = StreamCodec.composite(
                com.questclusters.data.ScoringCodecs.ENTRIES, ScoringSync::entries,
                com.questclusters.data.ScoringCodecs.SETTINGS, ScoringSync::settings,
                ScoringSync::new);

        @Override
        public Type<ScoringSync> type() {
            return TYPE;
        }
    }

    public record OpenSettlement() implements CustomPacketPayload {
        public static final Type<OpenSettlement> TYPE = newType("open_settlement");
        public static final StreamCodec<ByteBuf, OpenSettlement> CODEC = StreamCodec.unit(new OpenSettlement());

        @Override
        public Type<OpenSettlement> type() {
            return TYPE;
        }
    }

    public record Leaderboard(List<com.questclusters.data.ScoreCalc.Row> rows) implements CustomPacketPayload {
        public static final Type<Leaderboard> TYPE = newType("leaderboard");
        public static final StreamCodec<ByteBuf, Leaderboard> CODEC = com.questclusters.data.ScoringCodecs.ROWS.map(Leaderboard::new, Leaderboard::rows);

        @Override
        public Type<Leaderboard> type() {
            return TYPE;
        }
    }

    public record SetScore(long questId, com.questclusters.data.ScoreEntry entry) implements CustomPacketPayload {
        public static final Type<SetScore> TYPE = newType("set_score");
        public static final StreamCodec<ByteBuf, SetScore> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, SetScore::questId,
                com.questclusters.data.ScoringCodecs.ENTRY, SetScore::entry,
                SetScore::new);

        @Override
        public Type<SetScore> type() {
            return TYPE;
        }
    }

    public record SetSettlement(com.questclusters.data.SettlementSettings settings) implements CustomPacketPayload {
        public static final Type<SetSettlement> TYPE = newType("set_settlement");
        public static final StreamCodec<ByteBuf, SetSettlement> CODEC = com.questclusters.data.ScoringCodecs.SETTINGS.map(SetSettlement::new, SetSettlement::settings);

        @Override
        public Type<SetSettlement> type() {
            return TYPE;
        }
    }

    public record RequestLeaderboard() implements CustomPacketPayload {
        public static final Type<RequestLeaderboard> TYPE = newType("request_leaderboard");
        public static final StreamCodec<ByteBuf, RequestLeaderboard> CODEC = StreamCodec.unit(new RequestLeaderboard());

        @Override
        public Type<RequestLeaderboard> type() {
            return TYPE;
        }
    }
}
