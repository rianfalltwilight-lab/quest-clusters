package com.questclusters.editor.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ftb.mods.ftbquests.events.QuestProgressEventData;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.task.Task;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A task with "disable toast" set also keeps the quest it completes from showing a toast. */
@Mixin(value = Task.class, remap = false)
public abstract class TaskToastMixin {
    @WrapOperation(method = "onCompleted", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/quest/Quest;onCompleted(Ldev/ftb/mods/ftbquests/events/QuestProgressEventData;)V"))
    private void quest_clusters_editor$silenceQuest(Quest quest, QuestProgressEventData<?> data, Operation<Void> op) {
        boolean taskSilent = ((QuestObjectToastAccessor) this).quest_clusters_editor$disableToast();
        QuestObjectToastAccessor q = (QuestObjectToastAccessor) (Object) quest;
        boolean before = q.quest_clusters_editor$disableToast();
        if (taskSilent) q.quest_clusters_editor$setDisableToast(true);
        try {
            op.call(quest, data);
        } finally {
            q.quest_clusters_editor$setDisableToast(before);
        }
    }
}
