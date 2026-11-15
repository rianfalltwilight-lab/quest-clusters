package com.questclusters.xmod.kubejs;

import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.api.event.CustomFilterDisplayItemsEvent;
import dev.ftb.mods.ftbquests.events.CustomRewardEvent;
import dev.ftb.mods.ftbquests.events.CustomTaskEvent;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.events.ObjectProgressEvent;
import dev.ftb.mods.ftbquests.events.ObjectStartedEvent;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.AttachedData;
import net.minecraft.world.entity.player.Player;

import java.util.function.Function;

/**
 * KubeJS plugin (kubejs.plugins.txt): FTB Quests events, the FTBQuests binding and player.data.ftbquests.
 * KubeJS 6 (Minecraft 1.20.1).
 */
public class QuestsKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void init() {
        CustomTaskEvent.EVENT.register(e -> QuestsEvents.CUSTOM_TASK.hasListeners()
                ? QuestsEvents.CUSTOM_TASK.post(ScriptType.SERVER, e.getTask().toString(), new CustomTaskKubeEvent(e)).arch()
                : EventResult.pass());
        CustomRewardEvent.EVENT.register(e -> QuestsEvents.CUSTOM_REWARD.hasListeners()
                ? QuestsEvents.CUSTOM_REWARD.post(ScriptType.SERVER, e.getReward().toString(), new CustomRewardKubeEvent(e)).arch()
                : EventResult.pass());
        ObjectCompletedEvent.GENERIC.register(e -> progress(QuestsEvents.COMPLETED, e, ProgressKubeEvent.Completed::new));
        ObjectStartedEvent.GENERIC.register(e -> progress(QuestsEvents.STARTED, e, ProgressKubeEvent.Started::new));
        CustomFilterDisplayItemsEvent.ADD_ITEMSTACK.register(e -> {
            if (QuestsEvents.CUSTOM_FILTER_ITEM.hasListeners()) {
                QuestsEvents.CUSTOM_FILTER_ITEM.post(ScriptType.CLIENT, new CustomFilterItemKubeEvent(e));
            }
        });
    }

    /** Posts for the object's id and for each of its tags ("#tag"); handlers without a target see every object. */
    private static <E extends ObjectProgressEvent<?>> EventResult progress(EventHandler handler, E e, Function<E, ? extends EventJS> make) {
        // FTB Quests also fires these on the client for its local copy of the book: server scripts only see real ones
        if (!handler.hasListeners() || !e.getData().getFile().isServerSide()) return EventResult.pass();
        QuestObject object = e.getObject();
        EventJS kjs = make.apply(e);
        handler.post(ScriptType.SERVER, object.toString(), kjs); // listeners without a target, and for this id
        var tags = object.getTags();
        if (!tags.isEmpty()) { // "#tag" listeners: called directly, so untargeted listeners do not run once per tag
            handler.forEachListener(ScriptType.SERVER, c -> {
                if (c.extraId instanceof String t && t.startsWith("#") && tags.contains(t.substring(1))) {
                    try {
                        c.handle(kjs, (event, container, ex) -> ex);
                    } catch (dev.latvian.mods.kubejs.event.EventExit ignored) {
                        // the script ended the event early
                    } catch (Throwable ex) {
                        ScriptType.SERVER.console.error("Error in FTBQuestsEvents." + handler.name + "('" + t + "')", ex);
                    }
                }
            });
        }
        return EventResult.pass();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("FTBQuests", QuestsBinding.INSTANCE);
    }

    @Override
    public void attachPlayerData(AttachedData<Player> event) {
        event.add("ftbquests", new TeamDataJS.OfPlayer(event.getParent()));
    }

    @Override
    public void registerEvents() {
        QuestsEvents.GROUP.register();
    }
}
