package com.questclusters.server;

import com.questclusters.data.QuestCluster;
import com.questclusters.mixin.TeamDataAccessor;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.util.QuestKey;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Set;
import java.util.function.Supplier;

/**
 * One player's state of the personal quests (or a team's state from before they became personal): task progress,
 * start and completion times, finished repeat rounds, repeat cooldowns and claimed rewards.
 */
public final class PersonalState {
    final Long2LongMap progress = new Long2LongOpenHashMap();
    final Long2LongMap started = new Long2LongOpenHashMap();
    final Long2LongMap completed = new Long2LongOpenHashMap();
    final Long2IntMap count = new Long2IntOpenHashMap();
    final Long2LongMap repeat = new Long2LongOpenHashMap();
    final Object2LongMap<QuestKey> claimed = new Object2LongOpenHashMap<>();

    /** The entries of {@code ids} (quests and tasks) and {@code rewards} in a team's data. */
    public static PersonalState of(TeamData td, Set<Long> ids, Set<Long> rewards) {
        TeamDataAccessor a = (TeamDataAccessor) td;
        PersonalState s = new PersonalState();
        copy(a.quest_clusters$taskProgress(), s.progress, ids);
        copy(a.quest_clusters$started(), s.started, ids);
        copy(a.quest_clusters$completed(), s.completed, ids);
        copy(a.quest_clusters$repeatTime(), s.repeat, ids);
        Long2IntMap count = a.quest_clusters$completionCount();
        for (long id : ids) {
            if (count.containsKey(id)) s.count.put(id, count.get(id));
        }
        if (!rewards.isEmpty()) {
            a.quest_clusters$claimed().object2LongEntrySet().forEach(e -> {
                if (rewards.contains(e.getKey().id())) s.claimed.put(e.getKey(), e.getLongValue());
            });
        }
        return s;
    }

    /** Looks up only the personal ids: the team's maps can hold thousands of entries and this runs every tick. */
    private static void copy(Long2LongMap from, Long2LongMap to, Set<Long> ids) {
        for (long id : ids) {
            if (from.containsKey(id)) to.put(id, from.get(id));
        }
    }

    /** Removes the entries of {@code ids} and {@code rewards} from a team's data. */
    public static void clear(TeamData td, Set<Long> ids, Set<Long> rewards) {
        TeamDataAccessor a = (TeamDataAccessor) td;
        ids.forEach(id -> {
            a.quest_clusters$taskProgress().remove((long) id);
            a.quest_clusters$started().remove((long) id);
            a.quest_clusters$completed().remove((long) id);
            a.quest_clusters$repeatTime().remove((long) id);
            a.quest_clusters$completionCount().remove((long) id);
        });
        if (!rewards.isEmpty()) a.quest_clusters$claimed().keySet().removeIf(k -> rewards.contains(k.id()));
    }

    /** Writes this state into a team's data (after {@link #clear}). */
    public void apply(TeamData td) {
        TeamDataAccessor a = (TeamDataAccessor) td;
        a.quest_clusters$taskProgress().putAll(progress);
        a.quest_clusters$started().putAll(started);
        a.quest_clusters$completed().putAll(completed);
        a.quest_clusters$repeatTime().putAll(repeat);
        a.quest_clusters$completionCount().putAll(count);
        a.quest_clusters$claimed().putAll(claimed);
        td.clearCachedProgress();
    }

    public PersonalState copy() {
        PersonalState s = new PersonalState();
        s.progress.putAll(progress);
        s.started.putAll(started);
        s.completed.putAll(completed);
        s.count.putAll(count);
        s.repeat.putAll(repeat);
        s.claimed.putAll(claimed);
        return s;
    }

    public boolean isEmpty() {
        return progress.isEmpty() && started.isEmpty() && completed.isEmpty() && count.isEmpty() && repeat.isEmpty() && claimed.isEmpty();
    }

    // ------------------------------------------------------------------ NBT

    public CompoundTag write(Supplier<CompoundTag> factory) {
        CompoundTag t = factory.get();
        t.put("progress", longs(progress, factory));
        t.put("started", longs(started, factory));
        t.put("completed", longs(completed, factory));
        t.put("repeat", longs(repeat, factory));
        CompoundTag c = factory.get();
        count.long2IntEntrySet().forEach(e -> c.putInt(QuestCluster.idToString(e.getLongKey()), e.getIntValue()));
        t.put("count", c);
        CompoundTag r = factory.get();
        claimed.object2LongEntrySet().forEach(e -> r.putLong(e.getKey().uuid() + ":" + QuestCluster.idToString(e.getKey().id()), e.getLongValue()));
        t.put("claimed", r);
        return t;
    }

    public static PersonalState read(CompoundTag t) {
        PersonalState s = new PersonalState();
        readLongs(t.getCompound("progress"), s.progress);
        readLongs(t.getCompound("started"), s.started);
        readLongs(t.getCompound("completed"), s.completed);
        readLongs(t.getCompound("repeat"), s.repeat);
        CompoundTag c = t.getCompound("count");
        for (String k : c.getAllKeys()) s.count.put(QuestCluster.idFromString(k), c.getInt(k));
        CompoundTag r = t.getCompound("claimed");
        for (String k : r.getAllKeys()) {
            int colon = k.lastIndexOf(':');
            if (colon <= 0) continue;
            try {
                s.claimed.put(new QuestKey(java.util.UUID.fromString(k.substring(0, colon)), QuestCluster.idFromString(k.substring(colon + 1))),
                        r.getLong(k));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return s;
    }

    private static CompoundTag longs(Long2LongMap m, Supplier<CompoundTag> factory) {
        CompoundTag t = factory.get();
        m.long2LongEntrySet().forEach(e -> t.putLong(QuestCluster.idToString(e.getLongKey()), e.getLongValue()));
        return t;
    }

    private static void readLongs(CompoundTag t, Long2LongMap into) {
        for (String k : t.getAllKeys()) {
            if (t.contains(k, Tag.TAG_LONG)) into.put(QuestCluster.idFromString(k), t.getLong(k));
        }
    }
}
