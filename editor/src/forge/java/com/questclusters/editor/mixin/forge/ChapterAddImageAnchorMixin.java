package com.questclusters.editor.mixin.forge;

import com.questclusters.editor.compat.Images;
import com.questclusters.editor.data.CanvasElement;
import com.questclusters.platform.Ftbq;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 1.20.1: a copied guide point arrives with the id of the one it was copied from (copies are made from the image's
 * data, not as quest objects); it gets its own, so lines never confuse it with the original.
 */
@Mixin(value = Chapter.class, remap = false)
public abstract class ChapterAddImageAnchorMixin {
    @Inject(method = "addImage", at = @At("HEAD"))
    private void quest_clusters_editor$freshAnchor(ChapterImage img, CallbackInfo ci) {
        if (!(CanvasElement.parse(Ftbq.clickData(img)) instanceof CanvasElement.Anchor a) || a.uuid() == null) return;
        boolean[] taken = {false};
        ((Chapter) (Object) this).file.forAllChapters(ch -> {
            for (ChapterImage other : ch.getImages()) {
                if (other != img && CanvasElement.parse(Ftbq.clickData(other)) instanceof CanvasElement.Anchor b && a.uuid().equals(b.uuid())) {
                    taken[0] = true;
                }
            }
        });
        if (taken[0]) Images.setClickData(img, new CanvasElement.Anchor(UUID.randomUUID().toString()).encode());
    }
}
