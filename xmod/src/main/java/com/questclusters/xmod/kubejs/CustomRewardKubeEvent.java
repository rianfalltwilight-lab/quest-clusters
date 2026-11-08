package com.questclusters.xmod.kubejs;

import dev.ftb.mods.ftbquests.events.CustomRewardEvent;
import dev.ftb.mods.ftbquests.quest.reward.CustomReward;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.world.entity.player.Player;

/** A player claimed a custom reward. */
public class CustomRewardKubeEvent implements KubeEvent {
    private final CustomRewardEvent event;

    public CustomRewardKubeEvent(CustomRewardEvent event) {
        this.event = event;
    }

    public Player getEntity() {
        return event.getPlayer();
    }

    public Player getPlayer() {
        return event.getPlayer();
    }

    public CustomReward getReward() {
        return event.getReward();
    }

    public boolean getNotify() {
        return event.getNotify();
    }
}
