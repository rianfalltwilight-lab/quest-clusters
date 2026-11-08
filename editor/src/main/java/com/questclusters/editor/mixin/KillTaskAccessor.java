package com.questclusters.editor.mixin;

import dev.ftb.mods.ftbquests.quest.task.KillTask;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = KillTask.class, remap = false)
public interface KillTaskAccessor {
    @Accessor("entityTypeId")
    ResourceLocation quest_clusters_editor$entity();
}
