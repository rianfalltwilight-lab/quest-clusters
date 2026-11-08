package com.questclusters.editor.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.questclusters.editor.client.desc.ClientPersistentData;
import com.questclusters.editor.client.desc.DescriptionComponents;
import com.questclusters.editor.client.desc.TableWidget;
import com.questclusters.editor.data.DescriptionTable;
import dev.ftb.mods.ftblibrary.ui.BlankPanel;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import dev.ftb.mods.ftbquests.quest.Quest;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

/** Description lines: tables become grids, persistent-data placeholders show their value. */
@Mixin(value = ViewQuestPanel.class, remap = false)
public abstract class ViewQuestPanelDescMixin {
    @Shadow
    private Quest quest;

    @Inject(method = "addDescriptionText", at = @At("HEAD"))
    private void quest_clusters_editor$requestValues(boolean canEdit, Component subtitle, CallbackInfo ci) {
        if (quest == null) return;
        Set<String> keys = new HashSet<>();
        quest.getDescription().forEach(c -> DescriptionComponents.collectKeys(c, keys));
        ClientPersistentData.request(keys);
    }

    @org.spongepowered.asm.mixin.Unique
    private final java.util.Map<TextField, String> quest_clusters_editor$tables = new java.util.IdentityHashMap<>();

    @WrapOperation(method = "addDescriptionText", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftblibrary/ui/TextField;setText(Lnet/minecraft/network/chat/Component;)Ldev/ftb/mods/ftblibrary/ui/TextField;"))
    private TextField quest_clusters_editor$placeholder(TextField field, Component text, Operation<TextField> op) {
        String payload = DescriptionComponents.tablePayload(text);
        if (payload != null) quest_clusters_editor$tables.put(field, payload);
        return op.call(field, ClientPersistentData.substitute(text));
    }

    @WrapOperation(method = "addDescriptionText", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftblibrary/ui/BlankPanel;add(Ldev/ftb/mods/ftblibrary/ui/Widget;)V"))
    private void quest_clusters_editor$table(BlankPanel panel, Widget widget, Operation<Void> op) {
        if (widget instanceof TextField tf) {
            String payload = quest_clusters_editor$tables.remove(tf);
            DescriptionTable table = payload == null ? null : DescriptionTable.decode(payload);
            if (table != null) {
                op.call(panel, new TableWidget(panel, table, payload, quest, (ViewQuestPanel) (Object) this));
                return;
            }
        }
        op.call(panel, widget);
    }
}
