package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.desc.InsertComponents;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "Insert component" (+) button in the description editor's toolbar, after the image and {} buttons. */
@Mixin(targets = "dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen$ToolbarPanel", remap = false)
public abstract class EditorToolbarMixin {
    @Unique
    private SimpleTextButton quest_clusters_editor$insert;

    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void quest_clusters_editor$add(CallbackInfo ci) {
        Panel self = (Panel) (Object) this;
        quest_clusters_editor$insert = new SimpleTextButton(self, Component.empty(), Icons.ADD) {
            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                MultilineTextEditorScreen screen = (MultilineTextEditorScreen) getGui();
                MultilineEditorAccessor acc = (MultilineEditorAccessor) screen;
                var box = acc.quest_clusters_editor$textBox();
                String sel = box.hasSelection() ? box.getSelectedText() : null;
                screen.openContextMenu(InsertComponents.menu(screen, sel, line -> acc.quest_clusters_editor$insertAtEndOfLine("\n" + line)));
            }

            @Override
            public void addMouseOverText(TooltipList list) {
                list.add(Component.translatable("quest_clusters_editor.insert.button"));
            }
        };
        self.add(quest_clusters_editor$insert);
    }

    /** FTB Quests puts its undo button at x=223; it moves one slot right to make room. */
    @Inject(method = "alignWidgets", at = @At("TAIL"))
    private void quest_clusters_editor$align(CallbackInfo ci) {
        if (quest_clusters_editor$insert == null) return;
        for (Widget w : ((Panel) (Object) this).getWidgets()) {
            if (w != quest_clusters_editor$insert && w.posX == 223 && w.posY == 1) w.setPos(239, 1);
        }
        quest_clusters_editor$insert.setPosAndSize(213, 1, 16, 16);
    }
}
