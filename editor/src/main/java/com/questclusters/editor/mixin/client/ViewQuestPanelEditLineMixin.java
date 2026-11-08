package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.desc.DescriptionLines;
import com.questclusters.editor.client.desc.InsertComponents;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import dev.ftb.mods.ftbquests.quest.Quest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Editing a description line that one of our components wrote reopens that component's page. */
@Mixin(value = ViewQuestPanel.class, remap = false)
public abstract class ViewQuestPanelEditLineMixin {
    @Shadow
    private Quest quest;

    @Inject(method = "editDescLine0", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_editor$reedit(Widget clicked, int line, Object type, CallbackInfo ci) {
        if (quest == null || line < 0 || line >= quest.getRawDescription().size()) return;
        ViewQuestPanel self = (ViewQuestPanel) (Object) this;
        String raw = quest.getRawDescription().get(line);
        if (InsertComponents.reedit(self.getGui(), raw, edited -> DescriptionLines.set(quest, self, line, edited))) {
            ci.cancel();
        }
    }
}
