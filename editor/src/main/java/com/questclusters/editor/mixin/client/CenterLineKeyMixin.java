package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.desc.DescriptionLines;
import com.questclusters.editor.client.desc.LineRender;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Edit mode: Shift+O over a description line toggles its centring ({@code [center]} at the start of the line). */
@Mixin(value = QuestScreen.class, remap = false)
public abstract class CenterLineKeyMixin {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void quest_clusters_editor$toggleCenter(Key key, CallbackInfoReturnable<Boolean> cir) {
        if (!key.is(GLFW.GLFW_KEY_O) || !key.modifiers.shift() || key.modifiers.control() || key.modifiers.alt()) return;
        QuestScreen screen = (QuestScreen) (Object) this;
        var file = ClientQuestFile.INSTANCE;
        var view = screen.viewQuestPanel;
        var quest = view == null ? null : view.getViewedQuest();
        if (file == null || !file.canEdit() || quest == null) return;
        var info = LineRender.hovered(screen);
        if (info == null || info.questId() != quest.id || info.line() < 0 || info.line() >= quest.getRawDescription().size()) return;
        String raw = quest.getRawDescription().get(info.line());
        String toggled = raw.startsWith("[center]") ? raw.substring("[center]".length()) : "[center]" + raw;
        DescriptionLines.set(quest, view, info.line(), toggled);
        cir.setReturnValue(true);
    }
}
