package com.questclusters.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.questclusters.client.ClusterViewState;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.gui.quests.ChapterImageButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** ChapterImageButton does not override shouldDraw/isEnabled, so we hook the base class and filter by type. */
@Mixin(value = Widget.class, remap = false)
public abstract class WidgetMixin {
    @ModifyReturnValue(method = "shouldDraw", at = @At("RETURN"))
    private boolean quest_clusters$hideCollapsedImage(boolean original) {
        if (original && (Object) this instanceof ChapterImageButton img) {
            return !ClusterViewState.isHiddenImage(img.moveAndDeleteFocus().getMovableID());
        }
        return original;
    }

    @ModifyReturnValue(method = "isEnabled", at = @At("RETURN"))
    private boolean quest_clusters$disableCollapsedImage(boolean original) {
        if (original && (Object) this instanceof ChapterImageButton img) {
            return !ClusterViewState.isHiddenImage(img.moveAndDeleteFocus().getMovableID());
        }
        return original;
    }
}
