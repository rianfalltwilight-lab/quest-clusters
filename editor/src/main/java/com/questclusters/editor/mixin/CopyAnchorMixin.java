package com.questclusters.editor.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.questclusters.editor.data.CanvasElement;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

/** A copied guide point gets its own id, so lines never confuse it with the original. */
@Mixin(value = QuestObjectBase.class, remap = false)
public abstract class CopyAnchorMixin {
    @ModifyReturnValue(method = "copy", at = @At("RETURN"))
    private static <T extends QuestObjectBase> T quest_clusters_editor$freshAnchor(T copied) {
        if (copied instanceof ChapterImage img && CanvasElement.parse(img.getClickAction().actionData()) instanceof CanvasElement.Anchor) {
            ((ChapterImageAccessor) (Object) img).quest_clusters_editor$setClickAction(
                    img.getClickAction().withData(new CanvasElement.Anchor(UUID.randomUUID().toString()).encode()));
        }
        return copied;
    }
}
