package com.questclusters.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.questclusters.client.gui.DecorativeLineWindow;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Widget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(value = Panel.class, remap = false)
public abstract class PanelMixin {
    @ModifyReturnValue(method = "getWidgets", at = @At("RETURN"))
    private List<Widget> quest_clusters$decorativeWindow(List<Widget> original) {
        return DecorativeLineWindow.filter((Panel) (Object) this, original);
    }
}
