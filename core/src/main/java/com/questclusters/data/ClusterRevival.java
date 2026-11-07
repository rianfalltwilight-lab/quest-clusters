package com.questclusters.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalLong;

/**
 * Remembers memberships that were pruned because their objects disappeared, so they come back when the objects
 * come back. FTB Quests' undo restores deleted quests with their old ids (and undoing a chapter move puts an
 * object back in its old chapter); without this, undoing "delete cluster and members" would restore the quests
 * but not the cluster.
 * <p>
 * Pure logic; entries expire after {@link #MAX_AGE_MS} or once {@link #MAX_ENTRIES} newer ones exist.
 */
public final class ClusterRevival {
    public static final long MAX_AGE_MS = 60L * 60L * 1000L;
    public static final int MAX_ENTRIES = 256;

    private record Entry(QuestCluster snapshot, long memberId, long time) {
    }

    private final Deque<Entry> entries = new ArrayDeque<>();

    /** Record what a prune just removed. */
    public void remember(ClusterPruner.Result result, long now) {
        for (QuestCluster c : result.removedClusters()) {
            for (long m : c.members()) {
                add(new Entry(c, m, now));
            }
        }
        result.droppedMembers().forEach((cluster, members) -> members.forEach(m -> add(new Entry(cluster, m, now))));
    }

    private void add(Entry e) {
        entries.addFirst(e);
        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
    }

    /**
     * Puts back members whose objects exist again in their old chapter (and are not in another cluster), recreating
     * the cluster if it is gone. Returns the clusters that changed.
     */
    public List<QuestCluster> revive(ClusterStore store, MemberLookup lookup, long now) {
        List<QuestCluster> changed = new ArrayList<>();
        for (Iterator<Entry> it = entries.iterator(); it.hasNext(); ) {
            Entry e = it.next();
            if (now - e.time() > MAX_AGE_MS) {
                it.remove();
                continue;
            }
            QuestCluster snap = e.snapshot();
            OptionalLong chapter = lookup.chapterOfMovable(e.memberId());
            if (chapter.isEmpty() || chapter.getAsLong() != snap.chapterId() || !lookup.chapterExists(snap.chapterId())) {
                continue; // still gone (or elsewhere): keep waiting
            }
            if (store.clusterOfMember(e.memberId()).isPresent()) {
                it.remove(); // someone else owns it now
                continue;
            }
            QuestCluster current = store.get(snap.id()).orElse(null);
            QuestCluster next;
            if (current == null) {
                next = snap.withMembers(List.of(e.memberId()));
            } else if (current.chapterId() != snap.chapterId()) {
                it.remove();
                continue;
            } else {
                // keep the original member order where possible
                List<Long> members = new ArrayList<>();
                for (long m : snap.members()) {
                    if (m == e.memberId() || current.members().contains(m)) {
                        members.add(m);
                    }
                }
                for (long m : current.members()) {
                    if (!members.contains(m)) {
                        members.add(m);
                    }
                }
                next = current.withMembers(members);
            }
            store.put(next);
            changed.removeIf(c -> c.id() == next.id());
            changed.add(next);
            it.remove();
        }
        return changed;
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }
}
