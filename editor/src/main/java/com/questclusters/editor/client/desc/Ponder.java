package com.questclusters.editor.client.desc;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Opening Ponder scenes from descriptions, through reflection (Ponder is optional). */
public final class Ponder {
    public static final String PREFIX = "quest_enhance:ponder/";

    private Ponder() {
    }

    public static boolean available() {
        try {
            Class.forName("net.createmod.ponder.foundation.ui.PonderUI");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void open(String itemId) {
        var mc = Minecraft.getInstance();
        if (!available()) {
            mc.gui.getChat().addMessage(Component.translatable("quest_clusters_editor.ponder.missing").withStyle(ChatFormatting.RED));
            return;
        }
        try {
            ResourceLocation rl = ResourceLocation.parse(itemId);
            Object screen = Class.forName("net.createmod.ponder.foundation.ui.PonderUI").getMethod("of", ResourceLocation.class).invoke(null, rl);
            mc.setScreen((Screen) screen);
        } catch (Throwable t) {
            mc.gui.getChat().addMessage(Component.translatable("quest_clusters_editor.ponder.failed", itemId).withStyle(ChatFormatting.RED));
        }
    }

    /** Items that have Ponder scenes (empty without Ponder). */
    @SuppressWarnings("unchecked")
    public static List<ResourceLocation> items() {
        List<ResourceLocation> out = new ArrayList<>();
        try {
            Object access = Class.forName("net.createmod.ponder.foundation.PonderIndex").getMethod("getSceneAccess").invoke(null);
            var exists = access.getClass().getMethod("doScenesExistForId", ResourceLocation.class);
            for (ResourceLocation id : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
                if ((Boolean) exists.invoke(access, id)) out.add(id);
            }
        } catch (Throwable ignored) {
        }
        out.sort(java.util.Comparator.comparing(ResourceLocation::toString));
        return out;
    }
}
