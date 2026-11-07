package com.questclusters.data;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ScoringTest {
    static ScoreCalc.Progress progress(Set<Long> done, Map<Long, Long> times) {
        return new ScoreCalc.Progress() {
            public boolean questExists(long id) { return id != 99; }
            public boolean isCompleted(long id) { return done.contains(id); }
            public OptionalLong completedAt(long id) { return times.containsKey(id) ? OptionalLong.of(times.get(id)) : OptionalLong.empty(); }
            public long chapterOf(long id) { return id < 10 ? 1 : 2; }
        };
    }

    @Test
    void totalsChaptersAndNodeOrder() {
        Map<Long, ScoreEntry> e = new LinkedHashMap<>();
        e.put(1L, new ScoreEntry(10, "First", false));
        e.put(2L, new ScoreEntry(20, "", false));
        e.put(11L, new ScoreEntry(30, "Second", false));
        e.put(12L, new ScoreEntry(0, "Story only", false));
        e.put(99L, new ScoreEntry(1000, "deleted quest", false)); // no longer exists: ignored
        var r = ScoreCalc.compute(e, progress(Set.of(1L, 11L, 12L), Map.of(1L, 500L, 11L, 100L, 12L, 300L)));
        assertEquals(40, r.total());
        assertEquals(60, r.max());
        assertEquals(List.of("Second", "Story only", "First"), r.nodes().stream().map(ScoreCalc.Node::text).toList(), "by completion time");
        assertArrayEquals(new int[]{10, 30}, r.perChapter().get(1L));
        assertArrayEquals(new int[]{30, 30}, r.perChapter().get(2L));
        assertEquals(66.666, r.percent(), 0.01);
    }

    @Test
    void penaltiesAndEmpty() {
        Map<Long, ScoreEntry> e = Map.of(1L, new ScoreEntry(-5, "", false));
        var r = ScoreCalc.compute(e, progress(Set.of(1L), Map.of()));
        assertEquals(-5, r.total());
        assertEquals(0, r.max());
        assertEquals(0D, r.percent());
        assertTrue(new ScoreEntry(0, "", false).isEmpty());
    }

    @Test
    void ranks() {
        var s = SettlementSettings.DEFAULT;
        assertEquals("&6S", s.rankFor(95));
        assertEquals("&eA", s.rankFor(75));
        assertEquals("&7C", s.rankFor(0));
        var parsed = SettlementSettings.parseRanks(List.of("50=B", "bad", "100=SS", "x=1"));
        var custom = new SettlementSettings("t", List.of(), List.of(), parsed, true, true);
        assertEquals("SS", custom.rankFor(100));
        assertEquals("", custom.rankFor(10), "below every threshold");
        assertEquals(List.of("100=SS", "50=B"), custom.rankLines());
    }

    @Test
    void leaderboardOrder() {
        var rows = ScoreCalc.sort(List.of(new ScoreCalc.Row("b", 5, 10, false), new ScoreCalc.Row("a", 5, 10, true),
                new ScoreCalc.Row("c", 7, 0, false), new ScoreCalc.Row("d", 5, 50, false)));
        assertEquals(List.of("c", "d", "a", "b"), rows.stream().map(ScoreCalc.Row::team).toList());
    }

    @Test
    void scoringFileRoundTrip() {
        Map<Long, ScoreEntry> e = new LinkedHashMap<>();
        e.put(0xD0L, new ScoreEntry(10, "&6第一步", false));
        e.put(0xD9L, new ScoreEntry(0, "", true));
        var settings = new SettlementSettings("&lT", List.of("a", "b"), List.of("end"),
                List.of(new SettlementSettings.Rank(80, "A"), new SettlementSettings.Rank(0, "C")), false, false);
        var tag = ScoringNbt.write(e, settings, net.minecraft.nbt.CompoundTag::new);
        assertEquals(e, ScoringNbt.readEntries(tag));
        assertEquals(settings, ScoringNbt.readSettings(tag));
        assertEquals(SettlementSettings.DEFAULT, ScoringNbt.readSettings(new net.minecraft.nbt.CompoundTag()), "missing section = defaults");
    }
}
