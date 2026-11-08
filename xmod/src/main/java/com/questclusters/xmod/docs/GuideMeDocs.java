package com.questclusters.xmod.docs;

import guideme.GuidesCommon;
import guideme.PageAnchor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** GuideME remembers the screen it was opened from and returns to it, so the quest book comes back by itself. */
final class GuideMeDocs {
    private GuideMeDocs() {
    }

    static void open(Player player, ResourceLocation book, @Nullable ResourceLocation page, String anchor) {
        if (page == null) {
            GuidesCommon.openGuide(player, book);
        } else {
            GuidesCommon.openGuide(player, book, new PageAnchor(page, anchor.isEmpty() ? null : anchor));
        }
    }
}
