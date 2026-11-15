package com.questclusters.editor.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.questclusters.editor.EditorConfig;
import com.questclusters.editor.perf.VisibilityCache;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import org.spongepowered.asm.mixin.Mixin;

/** Quest visibility for the client's own team is cached for one tick (see VisibilityCache). */
@Mixin(value = Quest.class, remap = false)
public abstract class QuestVisibilityCacheMixin {
    @WrapMethod(method = "isVisible")
    private boolean quest_clusters_editor$cached(TeamData data, Operation<Boolean> original) {
        var file = ClientQuestFile.INSTANCE;
        if (file == null || data != file.selfTeamData || !RenderSystem.isOnRenderThread() || !EditorConfig.VISIBILITY_CACHE.get()) {
            return original.call(data);
        }
        Quest self = (Quest) (Object) this;
        Boolean cached = VisibilityCache.get(self);
        if (cached != null) return cached;
        boolean v = original.call(data);
        VisibilityCache.put(self, v);
        return v;
    }
}
