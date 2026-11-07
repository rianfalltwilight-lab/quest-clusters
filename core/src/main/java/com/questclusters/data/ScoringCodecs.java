package com.questclusters.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScoringCodecs {
    private ScoringCodecs() {
    }

    private static final StreamCodec<ByteBuf, String> TEXT = ByteBufCodecs.stringUtf8(SettlementSettings.MAX_LINE_LENGTH * 4);

    public static final StreamCodec<ByteBuf, ScoreEntry> ENTRY = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ScoreEntry::score,
            ByteBufCodecs.stringUtf8(ScoreEntry.MAX_NODE_LENGTH * 4), ScoreEntry::node,
            ByteBufCodecs.BOOL, ScoreEntry::opensSettlement,
            ScoreEntry::new);

    private static final StreamCodec<ByteBuf, List<String>> LINES = TEXT.apply(ByteBufCodecs.list(SettlementSettings.MAX_LINES));

    public static final StreamCodec<ByteBuf, SettlementSettings> SETTINGS = new StreamCodec<>() {
        @Override
        public SettlementSettings decode(ByteBuf buf) {
            String title = TEXT.decode(buf);
            List<String> intro = LINES.decode(buf), outro = LINES.decode(buf), ranks = LINES.decode(buf);
            return new SettlementSettings(title, intro, outro, SettlementSettings.parseRanks(ranks), buf.readBoolean(), buf.readBoolean());
        }

        @Override
        public void encode(ByteBuf buf, SettlementSettings s) {
            TEXT.encode(buf, s.title());
            LINES.encode(buf, s.intro());
            LINES.encode(buf, s.outro());
            LINES.encode(buf, s.rankLines());
            buf.writeBoolean(s.showMax());
            buf.writeBoolean(s.leaderboard());
        }
    };

    public static final StreamCodec<ByteBuf, Map<Long, ScoreEntry>> ENTRIES = new StreamCodec<>() {
        @Override
        public Map<Long, ScoreEntry> decode(ByteBuf buf) {
            int n = ByteBufCodecs.VAR_INT.decode(buf);
            if (n < 0 || n > 100_000) throw new IllegalArgumentException("too many score entries: " + n);
            Map<Long, ScoreEntry> m = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) m.put(buf.readLong(), ENTRY.decode(buf));
            return m;
        }

        @Override
        public void encode(ByteBuf buf, Map<Long, ScoreEntry> m) {
            ByteBufCodecs.VAR_INT.encode(buf, m.size());
            m.forEach((id, e) -> {
                buf.writeLong(id);
                ENTRY.encode(buf, e);
            });
        }
    };

    public static final StreamCodec<ByteBuf, ScoreCalc.Row> ROW = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), ScoreCalc.Row::team,
            ByteBufCodecs.VAR_INT, ScoreCalc.Row::quests,
            ByteBufCodecs.VAR_INT, ScoreCalc.Row::score,
            ByteBufCodecs.BOOL, ScoreCalc.Row::own,
            ScoreCalc.Row::new);

    public static final StreamCodec<ByteBuf, List<ScoreCalc.Row>> ROWS = ROW.apply(ByteBufCodecs.list(512));

    static List<String> copy(List<String> l) {
        return new ArrayList<>(l);
    }
}
