package com.questclusters.mixin.client;

import com.questclusters.client.ClusterClipboard;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Priority below the default (1000) so these HEAD callbacks run before Quest Enhance's, which cancel the methods.
 * We never cancel, so FTB Quests or Quest Enhance still do the actual copying.
 */
@Mixin(value = QuestScreen.class, priority = 500, remap = false)
public abstract class QuestScreenClipboardMixin {
    @Inject(method = "copyObjectsToClipboard", at = @At("HEAD"))
    private void quest_clusters$rememberClusters(CallbackInfoReturnable<Boolean> cir) {
        QuestScreenAccessor acc = (QuestScreenAccessor) this;
        ClusterClipboard.onCopy(List.copyOf(acc.quest_clusters$getSelectedObjects()), acc.quest_clusters$getSelectedChapter());
    }

    @Inject(method = "pasteSelectedQuest", at = @At("HEAD"))
    private void quest_clusters$rebuildClusters(boolean withDeps, Chapter chapter, CallbackInfoReturnable<Boolean> cir) {
        var xy = ((QuestScreenAccessor) this).quest_clusters$invokeGetSnappedXY();
        ClusterClipboard.onPaste(chapter, xy.getFirst(), xy.getSecond());
    }
}
