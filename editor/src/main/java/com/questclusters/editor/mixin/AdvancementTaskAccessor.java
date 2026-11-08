package com.questclusters.editor.mixin;

import dev.ftb.mods.ftbquests.quest.task.AdvancementTask;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AdvancementTask.class, remap = false)
public interface AdvancementTaskAccessor {
    @Accessor("advancement")
    ResourceLocation quest_clusters_editor$advancement();
}
