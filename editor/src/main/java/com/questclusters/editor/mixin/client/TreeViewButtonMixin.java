package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.gui.QuestTreeScreen;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.gui.quests.OtherButtonsPanelTop;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.client.gui.quests.TabButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tree view button in the quest book's top-right column. */
@Mixin(value = OtherButtonsPanelTop.class, remap = false)
public abstract class TreeViewButtonMixin {
    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void quest_clusters_editor$treeButton(CallbackInfo ci) {
        Panel self = (Panel) (Object) this;
        QuestScreen screen = (QuestScreen) self.getGui();
        self.add(new TabButton(self, Component.translatable("quest_clusters_editor.tree"), ItemIcon.getItemIcon(Items.OAK_SAPLING)) {
            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                new QuestTreeScreen(screen).openGui();
            }
        });
    }
}
