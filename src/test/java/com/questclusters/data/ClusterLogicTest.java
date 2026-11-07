package com.questclusters.data;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ClusterLogicTest {
    static final long CH1 = 0x100, CH2 = 0x200;

    static QuestCluster cluster(long id, long chapter, Long... members) {
        return new QuestCluster(id, chapter, "Title", "", QuestCluster.DEFAULT_COLOR, true, 0, 0, 3, 1, List.of(members));
    }

    FakeLookup lookup() {
        return new FakeLookup().chapter(CH1).chapter(CH2)
                .movable(1, CH1).movable(2, CH1).movable(3, CH1).movable(9, CH2);
    }

    @Test
    void idStringRoundTrip() {
        for (long id : new long[]{1L, 0x7FFFFFFFFFFFFFFFL, 0xDEADBEEFL, -1L}) {
            assertEquals(id, QuestCluster.idFromString(QuestCluster.idToString(id)));
        }
        assertEquals(0L, QuestCluster.idFromString("zz"));
        assertEquals(0L, QuestCluster.idFromString(""));
        assertEquals(0x1AL, QuestCluster.idFromString("#1A"));
    }

    @Test
    void validClusterPasses() {
        assertEquals(Optional.empty(), ClusterValidator.validate(cluster(5, CH1, 1L, 2L), new ClusterStore(), lookup()));
    }

    @Test
    void rejectsBadInput() {
        ClusterStore store = new ClusterStore();
        FakeLookup l = lookup();
        assertEquals("no_chapter", key(cluster(5, 0x999, 1L), store, l));
        assertEquals("no_members", key(cluster(5, CH1), store, l));
        assertEquals("no_member", key(cluster(5, CH1, 42L), store, l));
        assertEquals("member_other_chapter", key(cluster(5, CH1, 9L), store, l));
        assertEquals("duplicate_member", key(cluster(5, CH1, 1L, 1L), store, l));
        assertEquals("bad_id", key(cluster(0, CH1, 1L), store, l));
        assertEquals("empty_title", key(new QuestCluster(5, CH1, "  ", "", 0, true, 0, 0, 3, 1, List.of(1L)), store, l));
        assertEquals("bad_size", key(new QuestCluster(5, CH1, "t", "", 0, true, 0, 0, Double.NaN, 1, List.of(1L)), store, l));
        assertEquals("bad_position", key(new QuestCluster(5, CH1, "t", "", 0, true, Double.POSITIVE_INFINITY, 0, 3, 1, List.of(1L)), store, l));
        assertEquals("title_too_long", key(new QuestCluster(5, CH1, "x".repeat(QuestCluster.MAX_TITLE_LENGTH + 1), "", 0, true, 0, 0, 3, 1, List.of(1L)), store, l));
    }

    @Test
    void memberCanOnlyBelongToOneCluster() {
        ClusterStore store = new ClusterStore();
        store.put(cluster(5, CH1, 1L, 2L));
        assertEquals("member_in_other_cluster", key(cluster(6, CH1, 2L, 3L), store, lookup()));
        // updating the same cluster with its own members is fine
        assertEquals(Optional.empty(), ClusterValidator.validate(cluster(5, CH1, 1L, 2L, 3L), store, lookup()));
    }

    @Test
    void cannotMoveClusterToOtherChapter() {
        ClusterStore store = new ClusterStore();
        store.put(cluster(5, CH1, 1L));
        assertEquals("chapter_changed", key(cluster(5, CH2, 9L), store, lookup()));
    }

    @Test
    void storeKeepsMemberIndexConsistent() {
        ClusterStore store = new ClusterStore();
        store.put(cluster(5, CH1, 1L, 2L));
        store.put(cluster(6, CH1, 2L, 3L)); // steals member 2
        assertEquals(List.of(1L), store.get(5).orElseThrow().members());
        assertEquals(6L, store.clusterOfMember(2L).orElseThrow().id());

        store.put(cluster(6, CH1, 3L)); // update drops member 2
        assertTrue(store.clusterOfMember(2L).isEmpty());

        store.remove(5);
        assertTrue(store.clusterOfMember(1L).isEmpty());
        assertEquals(1, store.byChapter(CH1).size());
        assertEquals(0, store.byChapter(CH2).size());
    }

    @Test
    void prunerDropsDeletedAndMovedMembers() {
        ClusterStore store = new ClusterStore();
        store.put(cluster(5, CH1, 1L, 2L, 3L));
        store.put(cluster(6, CH1, 4L));            // 4 never existed -> cluster becomes empty
        store.put(cluster(7, 0x999, 1000L));      // chapter deleted

        FakeLookup l = lookup();
        l.movables.remove(2L);                    // quest 2 deleted
        l.movables.put(3L, CH2);                  // quest 3 moved to another chapter

        ClusterPruner.Result r = ClusterPruner.prune(store, l);
        assertTrue(r.changed());
        assertEquals(List.of(1L), store.get(5).orElseThrow().members());
        assertTrue(store.get(6).isEmpty());
        assertTrue(store.get(7).isEmpty());
        assertEquals(2, r.removed().size());
        assertEquals(1, r.updated().size());
        assertTrue(store.clusterOfMember(3L).isEmpty());

        assertFalse(ClusterPruner.prune(store, l).changed(), "second prune is a no-op");
    }

    @Test
    void colorParsing() {
        assertEquals(0xFF112233, ClusterNbt.parseColor("#112233"));
        assertEquals(0x80112233, ClusterNbt.parseColor("#80112233"));
        assertEquals(QuestCluster.DEFAULT_COLOR, ClusterNbt.parseColor("nope"));
    }

    private static String key(QuestCluster c, ClusterStore s, MemberLookup l) {
        return ClusterValidator.validate(c, s, l).map(ValidationError::key).orElse(null);
    }

    @Test
    void revivalRestoresClusterAfterUndoDelete() {
        ClusterStore store = new ClusterStore();
        store.put(cluster(5, CH1, 1L, 2L));
        store.put(cluster(6, CH1, 3L));
        FakeLookup l = lookup();
        ClusterRevival rev = new ClusterRevival();

        // "delete cluster and members": both quests go, cluster 5 is pruned away; quest 3 moves to another chapter
        l.movables.remove(1L);
        l.movables.remove(2L);
        l.movables.put(3L, CH2);
        rev.remember(ClusterPruner.prune(store, l), 0);
        assertTrue(store.get(5).isEmpty());
        assertTrue(store.get(6).isEmpty());
        assertTrue(rev.revive(store, l, 10).isEmpty(), "nothing back yet");

        // undo: FTB Quests restores the quests with the same ids
        l.movable(1, CH1).movable(2, CH1);
        rev.revive(store, l, 20);
        assertEquals(List.of(1L, 2L), store.get(5).orElseThrow().members());

        // undoing the move brings 3 back into cluster 6
        l.movable(3, CH1);
        rev.revive(store, l, 30);
        assertEquals(List.of(3L), store.get(6).orElseThrow().members());
        assertEquals(0, rev.size());
    }

    @Test
    void revivalRestoresSingleMemberInOrderAndRespectsNewOwner() {
        ClusterStore store = new ClusterStore();
        store.put(cluster(5, CH1, 1L, 2L, 3L));
        FakeLookup l = lookup();
        ClusterRevival rev = new ClusterRevival();
        l.movables.remove(2L);
        rev.remember(ClusterPruner.prune(store, l), 0);
        assertEquals(List.of(1L, 3L), store.get(5).orElseThrow().members());
        l.movable(2, CH1);
        rev.revive(store, l, 1);
        assertEquals(List.of(1L, 2L, 3L), store.get(5).orElseThrow().members(), "original order kept");

        // if the object was put in another cluster meanwhile, do not steal it back
        l.movables.remove(2L);
        rev.remember(ClusterPruner.prune(store, l), 2);
        l.movable(2, CH1);
        store.put(cluster(7, CH1, 2L));
        rev.revive(store, l, 3);
        assertEquals(7L, store.clusterOfMember(2L).orElseThrow().id());
    }

    @Test
    void revivalEntriesExpire() {
        ClusterStore store = new ClusterStore();
        store.put(cluster(5, CH1, 1L));
        FakeLookup l = lookup();
        ClusterRevival rev = new ClusterRevival();
        l.movables.remove(1L);
        rev.remember(ClusterPruner.prune(store, l), 0);
        l.movable(1, CH1);
        rev.revive(store, l, ClusterRevival.MAX_AGE_MS + 1);
        assertTrue(store.get(5).isEmpty());
        assertEquals(0, rev.size());
    }
}
