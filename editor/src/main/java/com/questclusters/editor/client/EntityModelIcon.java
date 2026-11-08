package com.questclusters.editor.client;

import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** An icon showing a living entity's model (quest icons, kill tasks, completion toasts). */
public final class EntityModelIcon extends Icon {
    private static final Map<ResourceLocation, LivingEntity> CACHE = new HashMap<>();
    private static Level cacheLevel;
    private final ResourceLocation id;
    private final Icon fallback;

    public EntityModelIcon(ResourceLocation id, Icon fallback) {
        this.id = id;
        this.fallback = fallback;
    }

    /** The cached entity, or null if it is not a living entity / cannot be created here. */
    public static LivingEntity entity(ResourceLocation id) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return null;
        if (level != cacheLevel) {
            CACHE.clear();
            cacheLevel = level;
        }
        return CACHE.computeIfAbsent(id, k -> {
            try {
                Entity e = BuiltInRegistries.ENTITY_TYPE.getOptional(k).map(t -> t.create(level)).orElse(null);
                return e instanceof LivingEntity le ? le : null;
            } catch (Throwable t) {
                return null;
            }
        });
    }

    @Override
    public void draw(GuiGraphics g, int x, int y, int w, int h) {
        LivingEntity e = entity(id);
        if (e == null) {
            fallback.draw(g, x, y, w, h);
            return;
        }
        float bw = Math.max(0.3F, e.getBbWidth()), bh = Math.max(0.3F, e.getBbHeight());
        int scale = Math.max(1, (int) Math.floor(Math.min(w * 0.8F / bw, h * 0.85F / bh)));
        int cx = x + w / 2, cy = y + h / 2;
        // a slight turn towards the viewer, like an inventory preview
        InventoryScreen.renderEntityInInventoryFollowsAngle(g, x, y, x + w, y + h, scale, 0.0625F, cx - 14, cy + 4, e);
    }

    @Override
    public String toString() {
        return fallback.toString();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EntityModelIcon m && m.id.equals(id);
    }
}
