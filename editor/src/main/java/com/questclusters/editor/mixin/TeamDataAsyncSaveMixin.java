package com.questclusters.editor.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.questclusters.editor.EditorServerConfig;
import com.questclusters.editor.perf.AsyncTeamSave;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;

/** Team progress files are written in the background (the snapshot is still taken on the server thread). */
@Mixin(value = TeamData.class, remap = false)
public abstract class TeamDataAsyncSaveMixin {
    @WrapOperation(method = "saveIfChanged", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftblibrary/snbt/SNBT;write(Ljava/nio/file/Path;Lnet/minecraft/nbt/CompoundTag;)Z"))
    private boolean quest_clusters_editor$async(Path file, CompoundTag tag, Operation<Boolean> op) {
        if (!EditorServerConfig.ASYNC_TEAM_SAVE.get()) {
            return op.call(file, tag);
        }
        AsyncTeamSave.write(file, tag);
        return true;
    }
}
