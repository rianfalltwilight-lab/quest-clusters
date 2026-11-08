package com.questclusters.xmod.kubejs;

import dev.ftb.mods.ftbquests.events.ObjectProgressEvent;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.kubejs.player.EntityArrayList;
import net.minecraft.server.level.ServerPlayer;

/** A quest object (file, chapter, quest or task) was completed or started for a team. */
public abstract class ProgressKubeEvent implements KubeEvent {
    private final ObjectProgressEvent<?> event;

    ProgressKubeEvent(ObjectProgressEvent<?> event) {
        this.event = event;
    }

    public TeamDataJS getData() {
        return new TeamDataJS.Wrapper(event.getData());
    }

    public QuestObject getObject() {
        return event.getObject();
    }

    public EntityArrayList getNotifiedPlayers() {
        return new EntityArrayList(event.getNotifiedPlayers());
    }

    public EntityArrayList getOnlineMembers() {
        return new EntityArrayList(event.getOnlineMembers());
    }

    /** A player of the team who is notified (the one who completed it, when there is one), or null. */
    public ServerPlayer getPlayer() {
        var notified = event.getNotifiedPlayers();
        if (!notified.isEmpty()) return notified.getFirst();
        var online = event.getOnlineMembers();
        return online.isEmpty() ? null : online.getFirst();
    }

    public static final class Completed extends ProgressKubeEvent {
        Completed(ObjectProgressEvent<?> e) {
            super(e);
        }
    }

    public static final class Started extends ProgressKubeEvent {
        Started(ObjectProgressEvent<?> e) {
            super(e);
        }
    }
}
