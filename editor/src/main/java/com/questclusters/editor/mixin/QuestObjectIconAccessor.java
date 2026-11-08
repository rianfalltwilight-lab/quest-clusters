package com.questclusters.editor.mixin;

import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = QuestObjectBase.class, remap = false)
public interface QuestObjectIconAccessor {
    @Accessor("rawIcon")
    ItemStack quest_clusters_editor$rawIcon();
}
