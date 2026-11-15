package com.questclusters.server;

import com.questclusters.QuestClusters;
import com.questclusters.net.ClusterPayloads;
import com.questclusters.platform.Net;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Personal quests: in a team, each member has their own progress, completion and rewards for them.
 * <p>
 * FTB Quests keeps one progress record per team. Every time FTB Quests looks up a player's team data (it does so
 * before any task progress, claim or check for that player), the personal quests' entries of that record are
 * swapped for that player's own. The team's original entries are put back before FTB Quests saves, so its files
 * never hold anyone's personal state; members' states are kept in {@code <world>/data/quest_clusters_personal.snbt}.
 * Messages FTB Quests sends about a personal quest go only to the member whose state it is.
 * A member without a state of their own starts from the team's state from before the quest became personal.
 */
public final class PersonalProgress {
    private static final Map<UUID, PersonalState> STORE = new HashMap<>();
    private static Set<Long> ids = Set.of(), rewards = Set.of();
    private static boolean loaded, dirty;
    /** Depth of FTB Quests calls about a personal object: their messages go only to the member it belongs to. */
    private static int scope;

    private PersonalProgress() {
    }

    public static boolean active() {
        return !ids.isEmpty();
    }

    public static boolean isPersonalObject(long id) {
        return ids.contains(id);
    }

    public static boolean isPersonalReward(long id) {
        return rewards.contains(id);
    }

    /** The quest and task ids and reward ids that are personal, for a set of personal quest ids. */
    public static Set<Long>[] objects(BaseQuestFile file, Collection<Long> quests) {
        Set<Long> o = new HashSet<>(), r = new HashSet<>();
        for (long id : quests) {
            if (!(file.getBase(id) instanceof Quest q)) continue;
            o.add(q.id);
            for (Task t : q.getTasks()) o.add(t.id);
            for (Reward w : q.getRewards()) r.add(w.id);
        }
        @SuppressWarnings("unchecked") Set<Long>[] out = new Set[]{o, r};
        return out;
    }

    // ---------------------------------------------------------------- lifecycle (from ServerClusters)

    static void load(ServerQuestFile file) {
        if (!loaded) {
            STORE.clear();
            Path p = path();
            if (p != null && Files.exists(p)) {
                SNBTCompoundTag tag = SNBT.read(p);
                if (tag != null) {
                    for (String k : tag.getAllKeys()) {
                        try {
                            STORE.put(UUID.fromString(k), PersonalState.read(tag.getCompound(k)));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                } else {
                    QuestClusters.LOGGER.error("Could not parse {}", p);
                }
            }
            loaded = true;
            dirty = false;
        }
        rulesChanged(file);
    }

    /** Before FTB Quests saves: every team gets its own state back, members' states go to our file. */
    static void save(ServerQuestFile file) {
        unloadAll(file);
        if (!dirty) return;
        Path p = path();
        if (p == null) return;
        try {
            Files.createDirectories(p.getParent());
            SNBTCompoundTag tag = new SNBTCompoundTag();
            STORE.forEach((id, s) -> {
                if (!s.isEmpty()) tag.put(id.toString(), s.write(SNBTCompoundTag::new));
            });
            SNBT.write(p, tag);
            dirty = false;
        } catch (Exception e) {
            QuestClusters.LOGGER.error("Failed to save {}", p, e);
        }
    }

    static void unload() {
        STORE.clear();
        ids = Set.of();
        rewards = Set.of();
        loaded = dirty = false;
        scope = 0;
    }

    static void tick() {
        scope = 0; // a call that threw never got to leave its scope
    }

    private static Path path() {
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        return s == null ? null : s.getWorldPath(LevelResource.ROOT).resolve("data").resolve("quest_clusters_personal.snbt");
    }

    /** The set of personal quests changed (or the book was loaded): teams go back to their own state first. */
    public static void rulesChanged(BaseQuestFile file) {
        unloadAll(file);
        Set<Long>[] o = objects(file, ServerLimits.limits().personalQuests());
        ids = o[0];
        rewards = o[1];
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s != null) s.getPlayerList().getPlayers().forEach(p -> sendTo(p, true));
    }

    /** Every team back to its own state (before the set of personal quests changes). */
    public static void unloadAll(BaseQuestFile file) {
        for (TeamData td : file.getAllTeamData()) unload(td);
    }

    // ---------------------------------------------------------------- swapping (from the FTB Quests mixins)

    /** FTB Quests looked up a player's team data on the server: put that player's state in. */
    public static void enter(TeamData td, Player player) {
        if (ids.isEmpty() || td == null || player == null || !td.getFile().isServerSide()) return;
        swap(td, player.getUUID());
    }

    /** Self-test only: put any member id's state in (there is only one real player in a test world). */
    public static void enterAs(TeamData td, UUID player) {
        if (!ids.isEmpty()) swap(td, player);
    }

    private static void swap(TeamData td, UUID player) {
        PersonalSlot slot = (PersonalSlot) td;
        UUID current = slot.quest_clusters$loaded();
        if (player.equals(current)) return;
        if (current == null) {
            slot.quest_clusters$setNeutral(PersonalState.of(td, ids, rewards));
        } else {
            STORE.put(current, PersonalState.of(td, ids, rewards));
            dirty = true;
        }
        PersonalState.clear(td, ids, rewards);
        PersonalState mine = STORE.get(player);
        (mine != null ? mine : slot.quest_clusters$neutral()).copy().apply(td);
        slot.quest_clusters$setLoaded(player);
    }

    /** Back to the team's own state (before FTB Quests saves it, or when the rules change). */
    public static void unload(TeamData td) {
        PersonalSlot slot = (PersonalSlot) td;
        UUID current = slot.quest_clusters$loaded();
        if (current == null) return;
        STORE.put(current, PersonalState.of(td, ids, rewards));
        dirty = true;
        PersonalState.clear(td, ids, rewards);
        if (slot.quest_clusters$neutral() != null) slot.quest_clusters$neutral().copy().apply(td);
        slot.quest_clusters$setLoaded(null);
        td.clearCachedProgress();
    }

    /** FTB Quests reloaded a team's data from its file: nothing of ours is in it. */
    public static void reset(TeamData td) {
        ((PersonalSlot) td).quest_clusters$setLoaded(null);
        ((PersonalSlot) td).quest_clusters$setNeutral(null);
    }

    public static void enterScope(long id) {
        if (ids.contains(id)) scope++;
    }

    public static void enterRewardScope(long id) {
        if (rewards.contains(id)) scope++;
    }

    public static void leaveScope(long id, boolean reward) {
        if ((reward ? rewards : ids).contains(id) && scope > 0) scope--;
    }

    /** Recipients of a FTB Quests message about the team's progress: only the owner, if it is about a personal object. */
    public static Collection<ServerPlayer> recipients(TeamData td, Collection<ServerPlayer> members) {
        if (scope == 0) return members;
        UUID owner = ((PersonalSlot) td).quest_clusters$loaded();
        if (owner == null) return members;
        return members.stream().filter(p -> p.getUUID().equals(owner)).toList();
    }

    // ---------------------------------------------------------------- sync and commands

    /** Sends a player their own state (after FTB Quests' own sync of the team's data). */
    public static void sendTo(ServerPlayer player) {
        sendTo(player, false);
    }

    /** @param full also FTB Quests' data of the team (the set of personal quests changed: the client's copy is off) */
    public static void sendTo(ServerPlayer player, boolean full) {
        var file = ServerQuestFile.INSTANCE;
        if (file == null) return;
        PersonalState s;
        if (ids.isEmpty()) {
            s = new PersonalState();
        } else {
            TeamData td = file.getOrCreateTeamData(player); // swaps the player's state in
            s = PersonalState.of(td, ids, rewards);
        }
        if (full) com.questclusters.platform.Ftbq.syncTeamData(player, file.getOrCreateTeamData(player));
        Net.sendToPlayer(player, new ClusterPayloads.PersonalSync(s.write(net.minecraft.nbt.CompoundTag::new)));
    }

    /** Admin: forgets a player's personal state (they start over from the team's earlier state). */
    public static boolean reset(ServerPlayer player) {
        var file = ServerQuestFile.INSTANCE;
        if (file == null) return false;
        TeamData td = file.getOrCreateTeamData(player);
        PersonalSlot slot = (PersonalSlot) td;
        boolean had = STORE.remove(player.getUUID()) != null || player.getUUID().equals(slot.quest_clusters$loaded());
        if (player.getUUID().equals(slot.quest_clusters$loaded())) {
            PersonalState.clear(td, ids, rewards);
            if (slot.quest_clusters$neutral() != null) slot.quest_clusters$neutral().copy().apply(td);
            slot.quest_clusters$setLoaded(null);
        }
        dirty = true;
        sendTo(player, true);
        return had;
    }
}
