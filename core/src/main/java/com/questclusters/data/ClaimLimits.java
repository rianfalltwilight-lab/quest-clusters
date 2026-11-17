package com.questclusters.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Limits on repeatable quests and their rewards, and how often each has been used.
 * <ul>
 *   <li>Rules (part of the quest book): a quest's maximum number of completions per team; a reward's maximum number
 *       of claims per team (all members together) and per player. 0 = no limit.</li>
 *   <li>Batch submit (part of the quest book): a repeatable quest whose tasks all take items may be submitted and
 *       finished for up to this many rounds in one go (shift-click). 0 = off.</li>
 *   <li>Counts (part of the world): claims per team and per player, kept across repeats (FTB Quests forgets its own
 *       claim records every time a quest repeats).</li>
 * </ul>
 * Completions are not counted here: FTB Quests keeps a per-team count of finished repeat cycles.
 */
public final class ClaimLimits {
    public static final int MAX = 100_000;
    /** Most rounds one batch submit may run. */
    public static final int MAX_BATCH = 256;

    /** @param team claims for the whole team, {@code player} claims per player (0 = unlimited) */
    public record RewardLimit(int team, int player) {
        public static final RewardLimit NONE = new RewardLimit(0, 0);

        public RewardLimit {
            team = Math.max(0, Math.min(MAX, team));
            player = Math.max(0, Math.min(MAX, player));
        }

        public boolean isEmpty() {
            return team == 0 && player == 0;
        }
    }

    // rules
    private final Map<Long, Integer> questMax = new LinkedHashMap<>();
    private final java.util.Set<Long> personal = new java.util.LinkedHashSet<>();
    private final Map<Long, RewardLimit> rewards = new LinkedHashMap<>();
    private final Map<Long, Integer> batch = new LinkedHashMap<>();
    // counts
    private final Map<UUID, Map<Long, Integer>> teamClaims = new HashMap<>();
    private final Map<UUID, Map<Long, Integer>> playerClaims = new HashMap<>();

    // ------------------------------------------------------------------ rules

    public int questMax(long quest) {
        return questMax.getOrDefault(quest, 0);
    }

    public void setQuestMax(long quest, int max) {
        max = Math.max(0, Math.min(MAX, max));
        if (max == 0) questMax.remove(quest);
        else questMax.put(quest, max);
    }

    /** Rounds one batch submit of this quest may run (0 = batch submit is off). */
    public int batchMax(long quest) {
        return batch.getOrDefault(quest, 0);
    }

    public void setBatch(long quest, int rounds) {
        rounds = Math.max(0, Math.min(MAX_BATCH, rounds));
        if (rounds == 0) batch.remove(quest);
        else batch.put(quest, rounds);
    }

    public Map<Long, Integer> batchRules() {
        return batch;
    }

    public RewardLimit reward(long reward) {
        return rewards.getOrDefault(reward, RewardLimit.NONE);
    }

    public void setReward(long reward, RewardLimit limit) {
        if (limit == null || limit.isEmpty()) rewards.remove(reward);
        else rewards.put(reward, limit);
    }

    public Map<Long, Integer> questRules() {
        return questMax;
    }

    public Map<Long, RewardLimit> rewardRules() {
        return rewards;
    }

    public boolean hasRules() {
        return !questMax.isEmpty() || !rewards.isEmpty() || !personal.isEmpty() || !batch.isEmpty();
    }

    /** Personal quest: in a team, every member has their own progress, completion and rewards for it. */
    public boolean isPersonal(long quest) {
        return personal.contains(quest);
    }

    public void setPersonal(long quest, boolean on) {
        if (on) personal.add(quest);
        else personal.remove(quest);
    }

    public java.util.Set<Long> personalQuests() {
        return personal;
    }

    /**
     * Whether a repeatable quest may reset for another round.
     *
     * @param finishedCycles rounds the team has already finished and reset (FTB Quests' completion count); the round
     *                       that is ending now is not in it yet
     */
    public boolean mayRepeat(long quest, int finishedCycles) {
        int max = questMax(quest);
        return max == 0 || finishedCycles + 1 < max;
    }

    // ------------------------------------------------------------------ counts

    public int teamClaims(UUID team, long reward) {
        return teamClaims.getOrDefault(team, Map.of()).getOrDefault(reward, 0);
    }

    public int playerClaims(UUID player, long reward) {
        return playerClaims.getOrDefault(player, Map.of()).getOrDefault(reward, 0);
    }

    /** True if the team or the player has used up a reward. */
    public boolean exhausted(long reward, UUID team, UUID player) {
        RewardLimit l = reward(reward);
        return (l.team() > 0 && teamClaims(team, reward) >= l.team()) || (l.player() > 0 && playerClaims(player, reward) >= l.player());
    }

    public void countClaim(long reward, UUID team, UUID player) {
        teamClaims.computeIfAbsent(team, k -> new HashMap<>()).merge(reward, 1, Integer::sum);
        playerClaims.computeIfAbsent(player, k -> new HashMap<>()).merge(reward, 1, Integer::sum);
    }

    /** Forgets counts: of one reward (0 = all rewards) for a team and/or a player (null = skip). Returns how many were removed. */
    public int resetCounts(UUID team, UUID player, long reward) {
        int n = 0;
        n += reset(teamClaims, team, reward);
        n += reset(playerClaims, player, reward);
        return n;
    }

    private static int reset(Map<UUID, Map<Long, Integer>> map, UUID who, long reward) {
        if (who == null) return 0;
        Map<Long, Integer> m = map.get(who);
        if (m == null) return 0;
        int n;
        if (reward == 0L) {
            n = m.size();
            map.remove(who);
        } else {
            n = m.remove(reward) != null ? 1 : 0;
            if (m.isEmpty()) map.remove(who);
        }
        return n;
    }

    public Map<Long, Integer> teamCounts(UUID team) {
        return teamClaims.getOrDefault(team, Map.of());
    }

    public Map<Long, Integer> playerCounts(UUID player) {
        return playerClaims.getOrDefault(player, Map.of());
    }

    /** Replaces the counts of one team and one player (client side: only its own are known). */
    public void setOwnCounts(UUID team, Map<Long, Integer> forTeam, UUID player, Map<Long, Integer> forPlayer) {
        teamClaims.clear();
        playerClaims.clear();
        if (team != null && !forTeam.isEmpty()) teamClaims.put(team, new HashMap<>(forTeam));
        if (player != null && !forPlayer.isEmpty()) playerClaims.put(player, new HashMap<>(forPlayer));
    }

    public void clearRules() {
        questMax.clear();
        rewards.clear();
        personal.clear();
        batch.clear();
    }

    public void clearCounts() {
        teamClaims.clear();
        playerClaims.clear();
    }

    // ------------------------------------------------------------------ NBT

    public CompoundTag writeRules(Supplier<CompoundTag> factory) {
        CompoundTag root = factory.get();
        CompoundTag q = factory.get();
        questMax.forEach((id, max) -> q.putInt(QuestCluster.idToString(id), max));
        root.put("quests", q);
        CompoundTag r = factory.get();
        rewards.forEach((id, l) -> {
            CompoundTag t = factory.get();
            if (l.team() > 0) t.putInt("team", l.team());
            if (l.player() > 0) t.putInt("player", l.player());
            r.put(QuestCluster.idToString(id), t);
        });
        root.put("rewards", r);
        net.minecraft.nbt.ListTag p = new net.minecraft.nbt.ListTag();
        personal.forEach(id -> p.add(net.minecraft.nbt.StringTag.valueOf(QuestCluster.idToString(id))));
        root.put("personal", p);
        CompoundTag b = factory.get();
        batch.forEach((id, n) -> b.putInt(QuestCluster.idToString(id), n));
        root.put("batch", b);
        return root;
    }

    public void readRules(CompoundTag root) {
        clearRules();
        CompoundTag q = root.getCompound("quests");
        for (String k : q.getAllKeys()) setQuestMax(QuestCluster.idFromString(k), q.getInt(k));
        CompoundTag r = root.getCompound("rewards");
        for (String k : r.getAllKeys()) {
            CompoundTag t = r.getCompound(k);
            setReward(QuestCluster.idFromString(k), new RewardLimit(t.getInt("team"), t.getInt("player")));
        }
        net.minecraft.nbt.ListTag p = root.getList("personal", Tag.TAG_STRING);
        for (int i = 0; i < p.size(); i++) {
            long id = QuestCluster.idFromString(p.getString(i));
            if (id != 0L) personal.add(id);
        }
        CompoundTag b = root.getCompound("batch");
        for (String k : b.getAllKeys()) setBatch(QuestCluster.idFromString(k), b.getInt(k));
    }

    public CompoundTag writeCounts(Supplier<CompoundTag> factory) {
        CompoundTag root = factory.get();
        root.put("teams", counts(teamClaims, factory));
        root.put("players", counts(playerClaims, factory));
        return root;
    }

    public void readCounts(CompoundTag root) {
        clearCounts();
        readCounts(root.getCompound("teams"), teamClaims);
        readCounts(root.getCompound("players"), playerClaims);
    }

    private static CompoundTag counts(Map<UUID, Map<Long, Integer>> map, Supplier<CompoundTag> factory) {
        CompoundTag out = factory.get();
        map.forEach((who, m) -> {
            CompoundTag t = factory.get();
            m.forEach((id, n) -> t.putInt(QuestCluster.idToString(id), n));
            out.put(who.toString(), t);
        });
        return out;
    }

    private static void readCounts(CompoundTag tag, Map<UUID, Map<Long, Integer>> into) {
        for (String k : tag.getAllKeys()) {
            UUID who;
            try {
                who = UUID.fromString(k);
            } catch (IllegalArgumentException e) {
                continue;
            }
            CompoundTag t = tag.getCompound(k);
            Map<Long, Integer> m = new HashMap<>();
            for (String id : t.getAllKeys()) {
                if (t.contains(id, Tag.TAG_INT) && t.getInt(id) > 0) m.put(QuestCluster.idFromString(id), t.getInt(id));
            }
            if (!m.isEmpty()) into.put(who, m);
        }
    }
}
