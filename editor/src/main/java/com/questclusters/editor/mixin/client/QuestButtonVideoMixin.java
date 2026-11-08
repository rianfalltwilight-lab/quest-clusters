package com.questclusters.editor.mixin.client;

import com.questclusters.editor.EntityModels;
import com.questclusters.editor.client.video.VideoSupport;
import com.questclusters.editor.mixin.QuestObjectIconAccessor;
import com.questclusters.mixin.client.QuestButtonAccessor;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestLinkButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Quests with a node video get a small play button in the corner; clicking it plays the video. */
@Mixin(value = QuestButton.class, remap = false)
public abstract class QuestButtonVideoMixin {
    private String quest_clusters_editor$video() {
        if ((Object) this instanceof QuestLinkButton) return null;
        var q = ((QuestButtonAccessor) this).quest_clusters$getQuest();
        return EntityModels.videoOf(((QuestObjectIconAccessor) (Object) q).quest_clusters_editor$rawIcon());
    }

    private int[] quest_clusters_editor$rect() {
        Widget w = (Widget) (Object) this;
        int s = Math.max(8, Math.min(14, Math.min(w.width, w.height) / 2));
        return new int[]{w.getX() + w.width - s, w.getY() + w.height - s, s};
    }

    @Inject(method = "draw", at = @At("TAIL"))
    private void quest_clusters_editor$playButton(GuiGraphics g, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        if (quest_clusters_editor$video() == null) return;
        int s = Math.max(8, Math.min(14, Math.min(w, h) / 2));
        int bx = x + w - s, by = y + h - s;
        g.pose().pushPose();
        g.pose().translate(0, 0, 50);
        g.fill(bx, by, bx + s, by + s, 0xD0000000);
        var font = Minecraft.getInstance().font;
        g.drawString(font, "▶", bx + (s - font.width("▶")) / 2 + 1, by + (s - 8) / 2, 0xFFFFFFFF, false);
        g.pose().popPose();
    }

    @Inject(method = "onClicked", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_editor$playClick(MouseButton button, CallbackInfo ci) {
        String video = quest_clusters_editor$video();
        if (video == null || !button.isLeft() || Screen.hasControlDown() || Screen.hasAltDown()) return;
        Widget w = (Widget) (Object) this;
        int[] r = quest_clusters_editor$rect();
        int mx = w.getMouseX(), my = w.getMouseY();
        if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[2]) {
            VideoSupport.open(video);
            ci.cancel();
        }
    }
}
