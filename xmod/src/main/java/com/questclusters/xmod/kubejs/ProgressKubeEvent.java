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
        //? if neoforge {
        return new EntityArrayList(event.getNotifiedPlayers());
        //?} else {
        /*return players(event.getNotifiedPlayers());
        *///?}
    }

    public EntityArrayList getOnlineMembers() {
        //? if neoforge {
        return new EntityArrayList(event.getOnlineMembers());
        //?} else {
        /*return players(event.getOnlineMembers());
        *///?}
    }

    //? if forge {
    /*private static EntityArrayList players(java.util.Collection<ServerPlayer> players) {
        return new EntityArrayList(players.isEmpty() ? null : players.iterator().next().level(), players);
    }
    *///?}

    /** A player of the team who is notified (the one who completed it, when there is one), or null. */
    public ServerPlayer getPlayer() {
        var notified = event.getNotifiedPlayers();
        if (!notified.isEmpty()) return notified.get(0);
        var online = event.getOnlineMembers();
        return online.isEmpty() ? null : online.get(0);
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
