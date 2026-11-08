package com.questclusters.xmod.kubejs;

import dev.ftb.mods.ftbquests.events.CustomTaskEvent;
import dev.ftb.mods.ftbquests.quest.task.CustomTask;
import dev.latvian.mods.kubejs.event.KubeEvent;

/** A custom task is being set up: scripts give it its check, timer, button and max progress. */
public class CustomTaskKubeEvent implements KubeEvent {
    private final CustomTaskEvent event;

    public CustomTaskKubeEvent(CustomTaskEvent event) {
        this.event = event;
    }

    public CustomTask getTask() {
        return event.getTask();
    }

    public void setCheck(CustomTask.Check check) {
        event.getTask().setCheck(check);
    }

    public void setCheckTimer(int ticks) {
        event.getTask().setCheckTimer(ticks);
    }

    public void setEnableButton(boolean enabled) {
        event.getTask().setEnableButton(enabled);
    }

    public void setMaxProgress(long max) {
        event.getTask().setMaxProgress(max);
    }
}
