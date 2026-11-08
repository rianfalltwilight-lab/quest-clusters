package com.questclusters.xmod.kubejs;

import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.util.ProgressChange;
import dev.latvian.mods.kubejs.player.EntityArrayList;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.function.Consumer;

/** A team's quest progress for scripts (same methods as with FTB XMod Compat). Ids: number or code string. */
public abstract class TeamDataJS {
    public abstract BaseQuestFile getFile();

    public abstract TeamData getData();

    static long id(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o instanceof QuestObjectBase q) return q.id;
        return QuestObjectBase.parseCodeString(String.valueOf(o));
    }

    private QuestObjectBase object(Object id) {
        return getFile().getBase(id(id));
    }

    public boolean addProgress(Object id, long progress) {
        if (object(id) instanceof Task t) {
            getData().addProgress(t, progress);
            return true;
        }
        return false;
    }

    public void changeProgress(Object id, Consumer<ProgressChange> consumer) {
        QuestObjectBase o = object(id);
        if (o == null) return;
        ProgressChange change = new ProgressChange(o, new UUID(0L, 0L));
        consumer.accept(change);
        o.forceProgress(getData(), change);
    }

    public void reset(Object id) {
        changeProgress(id, c -> c.setReset(true));
    }

    public void complete(Object id) {
        changeProgress(id, c -> c.setReset(false));
    }

    public boolean isCompleted(Object id) {
        return object(id) instanceof QuestObject q && getData().isCompleted(q);
    }

    public boolean isStarted(Object id) {
        return object(id) instanceof QuestObject q && getData().isStarted(q);
    }

    public boolean canStartQuest(Object id) {
        return object(id) instanceof Quest q && getData().canStartTasks(q);
    }

    public int getRelativeProgress(Object id) {
        return object(id) instanceof QuestObject q ? getData().getRelativeProgress(q) : 0;
    }

    public long getTaskProgress(Object id) {
        return getData().getProgress(id(id));
    }

    public boolean getLocked() {
        return getData().isLocked();
    }

    public void setLocked(boolean locked) {
        getData().setLocked(locked);
    }

    public EntityArrayList getOnlineMembers() {
        return new EntityArrayList(getData().getOnlineMembers());
    }

    /** Wraps a team's data directly. */
    public static final class Wrapper extends TeamDataJS {
        private final TeamData data;

        public Wrapper(TeamData data) {
            this.data = data;
        }

        @Override
        public BaseQuestFile getFile() {
            return data.getFile();
        }

        @Override
        public TeamData getData() {
            return data;
        }
    }

    /** {@code player.data.ftbquests}: always the player's current team. */
    public static final class OfPlayer extends TeamDataJS {
        private final Player player;

        public OfPlayer(Player player) {
            this.player = player;
        }

        @Override
        public BaseQuestFile getFile() {
            return dev.ftb.mods.ftbquests.api.FTBQuestsAPI.api().getQuestFile(player.level().isClientSide());
        }

        @Override
        public TeamData getData() {
            return getFile().getOrCreateTeamData(player);
        }
    }
}
