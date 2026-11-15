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
        Component shown = ClientPersistentData.substitute(text);
        if (payload == null && quest != null && com.questclusters.editor.EditorConfig.MARKUP.get()) {
            var markup = com.questclusters.editor.client.desc.LineMarkup.parse(shown, com.questclusters.editor.EditorConfig.CENTER_ALL.get());
            int line = -1;
            var desc = quest.getDescription();
            for (int i = 0; i < desc.size(); i++) {
                if (desc.get(i) == text) {
                    line = i;
                    break;
                }
            }
            com.questclusters.editor.client.desc.LineRender.register(field, quest.id, line, markup);
            if (!markup.isPlain()) {
                var theme = field.getGui().getTheme();
                int pw = field.getParent() == null ? field.maxWidth : field.getParent().width;
                int avail = pw - com.questclusters.editor.client.desc.LineRender.indentLeft(markup, theme)
                        - com.questclusters.editor.client.desc.LineRender.indentRight(markup);
                field.setMaxWidth(Math.max(20, avail));
                if (markup.center()) field.addFlags(dev.ftb.mods.ftblibrary.ui.Theme.CENTERED);
                TextField out = op.call(field, markup.text());
                //? if neoforge {
                if (markup.scale() != 1F) field.setScale(markup.scale()).reflow();
                //?} else {
                /*if (markup.scale() != 1F) field.setScale(markup.scale()).resize(theme);
                *///?}
                return out;
            }
        }
        return op.call(field, shown);
    }

    @WrapOperation(method = "addDescriptionText", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftblibrary/ui/BlankPanel;add(Ldev/ftb/mods/ftblibrary/ui/Widget;)V"))
    private void quest_clusters_editor$table(BlankPanel panel, Widget widget, Operation<Void> op) {
        if (widget instanceof TextField tf) {
            quest_clusters_editor$layout(panel, tf);
            String payload = quest_clusters_editor$tables.remove(tf);
            DescriptionTable table = payload == null ? null : DescriptionTable.decode(payload);
            if (table != null) {
                op.call(panel, new TableWidget(panel, table, payload, quest, (ViewQuestPanel) (Object) this));
                return;
            }
        }
        op.call(panel, widget);
    }

    /** Places a marked-up line: indent and width for bars, boxes and lists; fixed heights for dividers and gaps. */
    @org.spongepowered.asm.mixin.Unique
    private void quest_clusters_editor$layout(BlankPanel panel, TextField tf) {
        var info = com.questclusters.editor.client.desc.LineRender.info(tf);
        if (info == null || info.markup().isPlain()) return;
        var m = info.markup();
        int pw = panel.width;
        switch (m.kind()) {
            case DIVIDER -> tf.setPosAndSize(0, tf.getPosY(), pw, m.thick() ? 6 : 5);
            case DOUBLE_DIVIDER -> tf.setPosAndSize(0, tf.getPosY(), pw, 7);
            case GAP -> {
                if (m.gap() >= 0) {
                    tf.setPosAndSize(0, tf.getPosY(), pw, m.gap());
                } else {
                    // negative: pull the next line up by shortening the one before
                    tf.setPosAndSize(0, tf.getPosY(), pw, 0);
                    var list = panel.getWidgets();
                    if (!list.isEmpty()) {
                        Widget prev = list.get(list.size() - 1);
                        prev.setHeight(Math.max(1, prev.height + m.gap()));
                    }
                }
            }
            default -> {
                var theme = tf.getGui().getTheme();
                int left = com.questclusters.editor.client.desc.LineRender.indentLeft(m, theme);
                int right = com.questclusters.editor.client.desc.LineRender.indentRight(m);
                int pad = com.questclusters.editor.client.desc.LineRender.padding(m);
                tf.setPosAndSize(left, tf.getPosY(), Math.max(10, pw - left - right), tf.height + pad * 2);
            }
        }
    }
}
