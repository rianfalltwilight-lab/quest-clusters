package com.questclusters.editor.mixin.client;

import com.questclusters.editor.EditorConfig;
import com.questclusters.editor.EntityModels;
import com.questclusters.editor.client.EntityModelIcon;
import com.questclusters.editor.mixin.KillTaskAccessor;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.task.KillTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entity models as icons: a quest with a "model" set, a kill task, and a quest without its own icon whose only task
 * is a kill task. Goes through FTB Quests' icon lookup, so buttons, the details title and toasts all show it.
 */
@Mixin(value = QuestObjectBase.class, remap = false)
public abstract class QuestIconMixin {
    @Inject(method = "getIcon", at = @At("RETURN"), cancellable = true)
    private void quest_clusters_editor$model(CallbackInfoReturnable<Icon> cir) {
        Icon base = cir.getReturnValue();
        if (base instanceof EntityModelIcon) return;
        QuestObjectBase self = (QuestObjectBase) (Object) this;
        ResourceLocation id = null;
        if (self instanceof Quest q) {
            var raw = ((com.questclusters.editor.mixin.QuestObjectIconAccessor) (Object) q).quest_clusters_editor$rawIcon();
            id = EntityModels.modelOf(raw);
            if (id == null && EntityModels.isVideoPlaceholder(raw)) {
                cir.setReturnValue(Icon.empty());
                return;
            }
            if (id == null && (raw.isEmpty() || EntityModels.isVideoPlaceholder(raw)) && EditorConfig.KILL_TASK_MODELS.get()
                    && q.getTasks().size() == 1 && q.getTasks().iterator().next() instanceof KillTask kt) {
                id = ((KillTaskAccessor) kt).quest_clusters_editor$entity();
            }
        } else if (self instanceof KillTask kt && EditorConfig.KILL_TASK_MODELS.get()) {
            id = ((KillTaskAccessor) kt).quest_clusters_editor$entity();
        }
        if (id != null && EntityModelIcon.entity(id) != null) {
            cir.setReturnValue(new EntityModelIcon(id, base));
        }
    }
}
