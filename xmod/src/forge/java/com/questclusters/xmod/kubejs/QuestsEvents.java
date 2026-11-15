package com.questclusters.xmod.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.Extra;

/**
 * Event group {@code FTBQuestsEvents}, with the same events and targets scripts used with FTB XMod Compat:
 * {@code customTask}, {@code customReward}, {@code completed}, {@code started} (server, optional target = object
 * id or "#tag") and {@code customFilterItem} (client). KubeJS 6 (Minecraft 1.20.1).
 */
public interface QuestsEvents {
    EventGroup GROUP = EventGroup.of("FTBQuestsEvents");
    EventHandler CUSTOM_TASK = GROUP.server("customTask", () -> CustomTaskKubeEvent.class).extra(Extra.STRING).hasResult();
    EventHandler CUSTOM_REWARD = GROUP.server("customReward", () -> CustomRewardKubeEvent.class).extra(Extra.STRING).hasResult();
    EventHandler COMPLETED = GROUP.server("completed", () -> ProgressKubeEvent.Completed.class).extra(Extra.STRING);
    EventHandler STARTED = GROUP.server("started", () -> ProgressKubeEvent.Started.class).extra(Extra.STRING);
    EventHandler CUSTOM_FILTER_ITEM = GROUP.client("customFilterItem", () -> CustomFilterItemKubeEvent.class);
}
