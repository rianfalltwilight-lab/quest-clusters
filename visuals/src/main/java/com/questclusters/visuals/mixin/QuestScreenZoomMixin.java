package com.questclusters.visuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.questclusters.visuals.client.Ease;
import com.questclusters.visuals.client.VisualsConfig;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smooth zoom: the mouse wheel sets a target and the quest canvas glides there. FTB Quests' own integer zoom is
 * set to the target at once (it is saved with the screen state); only button size and spacing follow the glide.
 */
@Mixin(value = QuestScreen.class, remap = false)
public abstract class QuestScreenZoomMixin {
    @Shadow
    int zoom;
    @Shadow
    MouseButton grabbed;
    @Shadow
    @Final
    public QuestPanel questPanel;

    @Unique
    private Ease quest_clusters_visuals$zoom;

    @Unique
    private Ease quest_clusters_visuals$ease() {
        if (quest_clusters_visuals$zoom == null) {
            quest_clusters_visuals$zoom = new Ease(zoom, 0.2F);
        }
        return quest_clusters_visuals$zoom;
    }

    @Inject(method = "addZoom", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_visuals$smoothZoom(double up, CallbackInfo ci) {
        if (!VisualsConfig.SMOOTH_SCROLLING.get()) {
            return;
        }
        Ease e = quest_clusters_visuals$ease();
        int target = (int) Mth.clamp(Math.round(e.target()) + up * 4, 4, 28);
        if (target != Math.round(e.target())) {
            grabbed = null;
            zoom = target;
            e.set(target);
        }
        ci.cancel();
    }

    @Inject(method = "drawBackground", at = @At("HEAD"))
    private void quest_clusters_visuals$glide(CallbackInfo ci) {
        Ease e = quest_clusters_visuals$ease();
        if (Math.round(e.target()) != zoom) {
            e.set(zoom, (float) zoom); // zoom changed elsewhere (reset, restore): jump
        }
        if (e.animating() || Math.abs(e.get() - zoom) > 0.001F) {
            questPanel.withPreservedPos(QuestPanel::resetScroll);
        }
    }

    @ModifyReturnValue(method = "getQuestButtonSize", at = @At("RETURN"))
    private double quest_clusters_visuals$size(double original) {
        return quest_clusters_visuals$scaled(original);
    }

    @ModifyReturnValue(method = "getQuestButtonSpacing", at = @At("RETURN"))
    private double quest_clusters_visuals$spacing(double original) {
        return quest_clusters_visuals$scaled(original);
    }

    @Unique
    private double quest_clusters_visuals$scaled(double original) {
        if (!VisualsConfig.SMOOTH_SCROLLING.get() || zoom == 0 || quest_clusters_visuals$zoom == null) {
            return original;
        }
        return original * quest_clusters_visuals$zoom.get() / zoom;
    }
}
