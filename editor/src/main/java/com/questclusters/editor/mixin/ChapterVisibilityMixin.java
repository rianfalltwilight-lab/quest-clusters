package com.questclusters.editor.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.questclusters.editor.EditorRules;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.TeamData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Chapter quest prerequisites: a chapter stays hidden until all of them are completed (not in edit mode). */
@Mixin(value = Chapter.class, remap = false)
public abstract class ChapterVisibilityMixin {
    @ModifyReturnValue(method = "isVisible", at = @At("RETURN"))
    private boolean quest_clusters_editor$prerequisites(boolean visible, TeamData data) {
        return visible && EditorRules.prerequisitesMet((Chapter) (Object) this, data);
    }
}
