package com.questclusters.editor.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.questclusters.editor.client.video.VideoSupport;
import com.questclusters.editor.compat.kubejs.KubeJSBridge;
import com.questclusters.editor.data.CanvasElement;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.ChapterImageButton;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.ImageClickAction;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Canvas text and video elements react to players: text fires the KubeJS event {@code QuestEnhanceEvents.click},
 * a video opens the player. (FTB Quests ignores images whose click action is "none" outside edit mode.)
 */
@Mixin(value = ChapterImageButton.class, remap = false)
public abstract class CanvasElementClickMixin {
    @Shadow
    @Final
    private ChapterImage chapterImage;

    private boolean quest_clusters_editor$interactive() {
        CanvasElement e = CanvasElement.parse(chapterImage.getClickAction().actionData());
        return e instanceof CanvasElement.Text || e instanceof CanvasElement.Video;
    }

    @WrapOperation(method = {"checkMouseOver", "mousePressed"}, at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/quest/ImageClickAction;isNone()Z"))
    private boolean quest_clusters_editor$special(ImageClickAction action, Operation<Boolean> op) {
        return op.call(action) && !quest_clusters_editor$interactive();
    }

    @Inject(method = "onClicked", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_editor$click(MouseButton button, CallbackInfo ci) {
        if (!button.isLeft() || Screen.hasControlDown() || Screen.hasAltDown()
                || ClientQuestFile.INSTANCE == null || ClientQuestFile.INSTANCE.canEdit()) {
            return;
        }
        switch (CanvasElement.parse(chapterImage.getClickAction().actionData())) {
            case CanvasElement.Text t -> {
                KubeJSBridge.textClicked(t.text(), chapterImage.getChapter().id, chapterImage.getX(), chapterImage.getY(),
                        chapterImage.getWidth(), chapterImage.getHeight());
                ci.cancel();
            }
            case CanvasElement.Video v -> {
                VideoSupport.open(v.path());
                ci.cancel();
            }
            case null, default -> {
            }
        }
    }
}
