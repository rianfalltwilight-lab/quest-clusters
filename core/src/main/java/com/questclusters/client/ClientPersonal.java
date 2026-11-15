package com.questclusters.client;

import com.questclusters.server.PersonalProgress;
import com.questclusters.server.PersonalState;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.nbt.CompoundTag;

import java.util.Set;

/**
 * This player's own state of the personal quests, kept in the client's copy of the team's data. FTB Quests replaces
 * that copy when it syncs the whole team (it then holds whichever member's state the server had in): the own state
 * is put back right after.
 */
public final class ClientPersonal {
    private static PersonalState state = new PersonalState();
    private static TeamData applied;
    private static boolean pending;
    private static int revision;

    private ClientPersonal() {
    }

    public static void set(CompoundTag tag) {
        state = PersonalState.read(tag);
        pending = true;
    }

    /** The set of personal quests changed: apply again with the new set. */
    public static void rulesChanged() {
        pending = true;
    }

    public static void clear() {
        state = new PersonalState();
        applied = null;
        pending = false;
    }

    public static int revision() {
        return revision;
    }

    public static void tick() {
        ClientQuestFile f = ClientQuestFile.INSTANCE;
        if (f == null || f.selfTeamData == null) return;
        TeamData td = f.selfTeamData;
        if (td == applied && !pending) return;
        Set<Long>[] o = PersonalProgress.objects(f, ClientLimits.limits().personalQuests());
        if (td != applied && applied != null && !pending && !o[0].isEmpty()) {
            state = PersonalState.of(applied, o[0], o[1]); // progress made since the last full state came in
        }
        if (!o[0].isEmpty()) {
            PersonalState.clear(td, o[0], o[1]);
            state.copy().apply(td);
            revision++;
        }
        applied = td;
        pending = false;
    }
}
