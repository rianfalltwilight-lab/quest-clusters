package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.ClientEditor;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import dev.ftb.mods.ftbquests.quest.Quest;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ViewQuestPanel.class, remap = false)
public abstract class ViewQuestPanelEditorMixin {
    @Shadow
    private Quest quest;

    /** Details background: over the theme's panel background, under the title, icon and borders. */
    @Inject(method = "drawBackground", at = @At(value = "INVOKE", ordinal = 0, shift = At.Shift.AFTER,
            target = "Ldev/ftb/mods/ftblibrary/icon/Icon;draw(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"))
    private void quest_clusters_editor$viewBackground(GuiGraphics g, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        if (quest != null) {
            String bg = ClientEditor.book().quest(quest.id).viewBackground();
            if (!bg.isEmpty()) {
                ClientEditor.icon(bg).draw(g, x, y, w, h);
            }
        }
    }

    /**
     * "Disable scroll paging": the wheel still scrolls the description, but never flips pages. Placed where FTB Quests
     * starts its paging check, i.e. after the description did not use the scroll.
     */
    @Inject(method = "mouseScrolled", cancellable = true, at = @At(value = "FIELD", ordinal = 0,
            opcode = org.objectweb.asm.Opcodes.GETFIELD,
            target = "Ldev/ftb/mods/ftbquests/client/gui/quests/ViewQuestPanel;lastScrollTime:J"))
    private void quest_clusters_editor$noPaging(double mx, double my, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (quest != null && ClientEditor.book().quest(quest.id).disableScrollPaging()) {
            cir.setReturnValue(true);
        }
    }
}
