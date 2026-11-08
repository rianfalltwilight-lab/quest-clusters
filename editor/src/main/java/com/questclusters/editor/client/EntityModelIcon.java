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

    /** A slight turn towards the viewer, like an inventory preview (vanilla's angle factors, about -1..1). */
    private static final float TURN_X = 0.35F, TURN_Y = -0.15F;

    @Override
    public void draw(GuiGraphics g, int x, int y, int w, int h) {
        LivingEntity e = entity(id);
        if (e == null) {
            fallback.draw(g, x, y, w, h);
            return;
        }
        float bw = Math.max(0.3F, e.getBbWidth()), bh = Math.max(0.3F, e.getBbHeight());
        float scale = Math.max(1F, Math.min(w * 0.8F / bw, h * 0.85F / bh));
        // drawn without a scissor: icons sit inside FTB panels that already clip, with their own transforms
        org.joml.Quaternionf pose = new org.joml.Quaternionf().rotateZ((float) Math.PI);
        org.joml.Quaternionf camera = new org.joml.Quaternionf().rotateX(TURN_Y * 20F * (float) (Math.PI / 180D));
        pose.mul(camera);
        float bodyRot = e.yBodyRot, yRot = e.getYRot(), xRot = e.getXRot(), headRotO = e.yHeadRotO, headRot = e.yHeadRot;
        e.yBodyRot = 180F + TURN_X * 20F;
        e.setYRot(180F + TURN_X * 40F);
        e.setXRot(-TURN_Y * 20F);
        e.yHeadRot = e.getYRot();
        e.yHeadRotO = e.getYRot();
        try {
            float s = e.getScale();
            InventoryScreen.renderEntityInInventory(g, x + w / 2F, y + h / 2F, scale / s,
                    new org.joml.Vector3f(0F, e.getBbHeight() / 2F, 0F), pose, camera, e);
        } finally {
            e.yBodyRot = bodyRot;
            e.setYRot(yRot);
            e.setXRot(xRot);
            e.yHeadRotO = headRotO;
            e.yHeadRot = headRot;
        }
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
