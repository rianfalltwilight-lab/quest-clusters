package com.questclusters.xmod.docs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.PatchouliAPI;

/**
 * Patchouli: the anchor is a page number inside the entry (empty or not a number: first page); no entry opens the
 * book's landing page. Patchouli closes to the game, so the screen it was opened from (the quest book) is
 * reopened when the Patchouli screen goes away.
 */
final class PatchouliDocs {
    private static @Nullable Screen returnTo;
    private static @Nullable Screen opened;
    private static boolean listening;

    private PatchouliDocs() {
    }

    static void open(Player player, ResourceLocation book, @Nullable ResourceLocation entry, String anchor) {
        if (!player.level().isClientSide()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Screen before = mc.screen;
        if (entry == null) {
            PatchouliAPI.get().openBookGUI(book);
        } else {
            PatchouliAPI.get().openBookEntry(book, entry, page(anchor));
        }
        Screen now = mc.screen;
        if (now != null && now != before && now.getClass().getName().startsWith("vazkii.patchouli")) {
            returnTo = before;
            opened = now;
            if (!listening) {
                listening = true;
                NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
            }
        }
    }

    static int page(String anchor) {
        try {
            return Math.max(0, Integer.parseInt(anchor.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void tick() {
        if (opened == null) {
            return;
        }
        Screen now = Minecraft.getInstance().screen;
        if (now == null) {
            Screen back = returnTo;
            returnTo = null;
            opened = null;
            if (back != null) {
                Minecraft.getInstance().setScreen(back);
            }
        } else if (!now.getClass().getName().startsWith("vazkii.patchouli")) {
            // something else took over (e.g. another link): don't pull the quest book back later
            returnTo = null;
            opened = null;
        }
    }
}
