package com.questclusters.xmod.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventTargetType;
import dev.latvian.mods.kubejs.event.TargetedEventHandler;

/**
 * Event group {@code FTBQuestsEvents}, with the same events and targets scripts used with FTB XMod Compat:
 * {@code customTask}, {@code customReward}, {@code completed}, {@code started} (server, optional target = object
 * id or "#tag") and {@code customFilterItem} (client).
 */
public interface QuestsEvents {
    EventGroup GROUP = EventGroup.of("FTBQuestsEvents");
    TargetedEventHandler<String> CUSTOM_TASK = GROUP.server("customTask", () -> CustomTaskKubeEvent.class)
            .supportsTarget(EventTargetType.STRING).hasResult();
    TargetedEventHandler<String> CUSTOM_REWARD = GROUP.server("customReward", () -> CustomRewardKubeEvent.class)
            .supportsTarget(EventTargetType.STRING).hasResult();
    TargetedEventHandler<String> COMPLETED = GROUP.server("completed", () -> ProgressKubeEvent.Completed.class)
            .supportsTarget(EventTargetType.STRING);
    TargetedEventHandler<String> STARTED = GROUP.server("started", () -> ProgressKubeEvent.Started.class)
            .supportsTarget(EventTargetType.STRING);
    EventHandler CUSTOM_FILTER_ITEM = GROUP.client("customFilterItem", () -> CustomFilterItemKubeEvent.class);
}
