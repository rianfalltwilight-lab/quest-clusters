package com.questclusters.api;

import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-side quest book lifecycle for Quest Clusters modules. Called on the server thread:
 * after FTB Quests loaded the book (and migrations ran), when it saves, when it unloads, when a player logs in,
 * and every server tick.
 */
public final class QuestBookHooks {
    public interface Listener {
        default void loaded(ServerQuestFile file) {
        }

        default void saving(ServerQuestFile file) {
        }

        default void unloaded() {
        }

        default void login(ServerPlayer player) {
        }

        default void tick(ServerQuestFile file) {
        }
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private QuestBookHooks() {
    }

    public static void register(Listener l) {
        LISTENERS.add(l);
    }

    public static void fireLoaded(ServerQuestFile f) {
        LISTENERS.forEach(l -> l.loaded(f));
    }

    public static void fireSaving(ServerQuestFile f) {
        LISTENERS.forEach(l -> l.saving(f));
    }

    public static void fireUnloaded() {
        LISTENERS.forEach(Listener::unloaded);
    }

    public static void fireLogin(ServerPlayer p) {
        LISTENERS.forEach(l -> l.login(p));
    }

    public static void fireTick(ServerQuestFile f) {
        LISTENERS.forEach(l -> l.tick(f));
    }
}
