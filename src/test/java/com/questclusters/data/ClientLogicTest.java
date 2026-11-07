package com.questclusters.data;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

import static com.questclusters.data.ClusterEdges.End.box;
import static com.questclusters.data.ClusterEdges.End.object;
import static org.junit.jupiter.api.Assertions.*;

class ClientLogicTest {
    static QuestCluster cluster(long id, boolean defaultExpanded, Long... members) {
        return new QuestCluster(id, 1, "t", "", 0, defaultExpanded, 0, 0, 3, 1, List.of(members));
    }

    @Test
    void expandStateOverridesDefault() {
        ExpandState s = new ExpandState();
        QuestCluster c = cluster(5, true, 1L);
        assertTrue(s.isExpanded(c));
        int v = s.version();
        assertTrue(s.setExpanded(c, false));
        assertFalse(s.isExpanded(c));
        assertEquals(Map.of(5L, false), s.overrides());
        assertTrue(s.version() > v);
        assertFalse(s.setExpanded(c, false), "no change");
        assertTrue(s.setExpanded(c, true));
        assertTrue(s.overrides().isEmpty(), "back to default is not stored");
    }

    @Test
    void hiddenOnlyWhenCollapsed() {
        ClusterStore store = new ClusterStore();
        QuestCluster c = cluster(5, false, 1L);
        store.put(c);
        ExpandState s = new ExpandState();
        assertTrue(s.isHidden(store, 1L));
        assertFalse(s.isHidden(store, 2L));
        s.setExpanded(c, true);
        assertFalse(s.isHidden(store, 1L));
    }

    static ClusterEdges.Node n(long id, long... deps) {
        Set<Long> d = new java.util.HashSet<>();
        for (long x : deps) d.add(x);
        return new ClusterEdges.Node(id, id, d, true, false, false);
    }

    @Test
    void edgesRerouteToBoxesAndDeduplicate() {
        // quest 10 and 11 are in collapsed cluster 100; 20 depends on both; 11 depends on 10 (internal); 10 depends on 30
        List<ClusterEdges.Node> nodes = List.of(n(10, 30), n(11, 10), n(20, 10, 11), n(30), n(40, 30));
        var edges = ClusterEdges.compute(nodes, id -> id == 10 || id == 11 ? OptionalLong.of(100) : OptionalLong.empty());

        assertEquals(Set.of(
                new ClusterEdges.Edge(box(100), object(30)),
                new ClusterEdges.Edge(object(20), box(100))
        ), Set.copyOf(edges));
        // 40 -> 30 has no cluster end: FTB Quests draws it, not us
    }

    @Test
    void boxToBoxAndHiddenFlags() {
        var a = n(1);
        var b = n(2, 1);
        var hidden = new ClusterEdges.Node(3, 3, Set.of(1L), true, true, false); // hides its own lines
        var invisible = new ClusterEdges.Node(4, 4, Set.of(1L), false, false, false);
        var edges = ClusterEdges.compute(List.of(a, b, hidden, invisible),
                id -> id == 1 ? OptionalLong.of(100) : id == 2 ? OptionalLong.of(200) : OptionalLong.empty());
        assertEquals(List.of(new ClusterEdges.Edge(box(200), box(100))), edges);
    }

    @Test
    void questLinkCountsAsCanvasObject() {
        // link 50 shows quest 10; quest 20 depends on 10 -> lines to both the quest and the link
        var q10 = n(10);
        var link = new ClusterEdges.Node(50, 10, Set.of(), true, false, false);
        var q20 = n(20, 10);
        var edges = ClusterEdges.compute(List.of(q10, link, q20), id -> id == 20 ? OptionalLong.of(7) : OptionalLong.empty());
        assertEquals(Set.of(new ClusterEdges.Edge(box(7), object(10)), new ClusterEdges.Edge(box(7), object(50))), Set.copyOf(edges));
    }

    @Test
    void mergedEdgeKeepsUnderlyingPairs() {
        // 20 depends on 10 and 11, both inside collapsed cluster 100 -> one merged edge standing for two real lines
        var edges = ClusterEdges.computeWithPairs(List.of(n(10), n(11), n(20, 10, 11)),
                id -> id == 10 || id == 11 ? OptionalLong.of(100) : OptionalLong.empty());
        assertEquals(1, edges.size());
        assertEquals(Set.of(new ClusterEdges.Pair(20, 10), new ClusterEdges.Pair(20, 11)),
                Set.copyOf(edges.get(new ClusterEdges.Edge(object(20), box(100)))));
    }

    @Test
    void lineVisibilityRule() {
        // all real lines hidden -> merged line hidden; any visible -> drawn; reveal-on-hover only when every hidden one allows it
        assertEquals(LineRule.Visibility.SHOW, LineRule.decide(2, 0, 0));
        assertEquals(LineRule.Visibility.SHOW, LineRule.decide(2, 1, 0));
        assertEquals(LineRule.Visibility.HIDDEN, LineRule.decide(2, 2, 0));
        assertEquals(LineRule.Visibility.HIDDEN_REVEAL_ON_HOVER, LineRule.decide(2, 2, 1));
    }
}
