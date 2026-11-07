package com.questclusters.visuals.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.questclusters.visuals.QuestClustersVisuals;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Icon that renders a live entity model ("entity:minecraft:zombie[rotate=true]{IsBaby:1b}"). Usable anywhere FTB
 * Library takes an icon string: quest and chapter icons, description images, reward icons. Invalid strings show a
 * barrier; outside a world nothing is drawn.
 */
public final class EntityIcon extends Icon {
    private static final Map<String, Icon> CACHE = new HashMap<>();
    private static final Icon INVALID = ItemIcon.getItemIcon(Items.BARRIER);

    private final EntityIconSpec spec;
    private final String raw;
    private Entity entity;
    private ClientLevel entityLevel;
    private boolean failed;

    private EntityIcon(EntityIconSpec spec, String raw) {
        this.spec = spec;
        this.raw = raw;
    }

    /** The original icon string: FTB Library uses it for JSON, hashCode and equals, and to save the icon. */
    @Override
    public String toString() {
        return raw;
    }

    /** Icon for an "entity:..." string (cached). */
    public static Icon of(String raw) {
        return CACHE.computeIfAbsent(raw, r -> {
            EntityIconSpec s = EntityIconSpec.parse(r);
            return s == null ? INVALID : new EntityIcon(s, r);
        });
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public EntityIconSpec spec() {
        return spec;
    }

    @Override
    public void draw(GuiGraphics g, int x, int y, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || w <= 0 || h <= 0) return;
        Entity e = entity(mc.level);
        if (e == null) {
            INVALID.draw(g, x, y, w, h);
            return;
        }
        float size = Math.min(w, h);
        float scale = size / Math.max(0.5F, Math.max(e.getBbHeight(), e.getBbWidth())) * 0.85F;

        // orientation: a three-quarter view, optionally spinning or following the cursor
        float yaw = 20F, pitch = 0F;
        if (spec.rotate()) {
            yaw = (System.currentTimeMillis() % 7200L) / 20F;
        } else if (spec.lookAtCursor()) {
            double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
            double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
            yaw = (float) Math.atan((x + w / 2D - mx) / 40D) * 40F;
            pitch = (float) Math.atan((y + h / 2D - my) / 40D) * 20F;
        }
        e.setYRot(180F + yaw);
        e.setXRot(-pitch);
        if (e instanceof LivingEntity le) {
            le.yBodyRot = 180F + yaw;
            le.yBodyRotO = le.yBodyRot;
            le.yHeadRot = le.getYRot();
            le.yHeadRotO = le.yHeadRot;
        }

        var ps = g.pose();
        ps.pushPose();
        ps.translate(x + w / 2F, y + h / 2F + e.getBbHeight() * scale / 2F, 150F);
        ps.scale(scale, scale, -scale);
        Quaternionf flip = Axis.ZP.rotationDegrees(180F);
        ps.mulPose(flip);
        EntityRenderDispatcher d = mc.getEntityRenderDispatcher();
        Quaternionf camera = Axis.XP.rotationDegrees(0F);
        camera.conjugate();
        d.overrideCameraOrientation(camera);
        d.setRenderShadow(false);
        Lighting.setupForEntityInInventory();
        try {
            RenderSystem.runAsFancy(() -> d.render(e, 0, 0, 0, 0F, 1F, ps, g.bufferSource(), 0xF000F0));
            g.flush();
        } catch (Exception ex) {
            failed = true; // some modded renderers can't draw outside the world: stop trying
            QuestClustersVisuals.LOGGER.warn("Entity icon {} failed to render: {}", spec.entityId(), ex.toString());
        } finally {
            d.setRenderShadow(true);
            Lighting.setupFor3DItems();
            ps.popPose();
        }
    }

    private Entity entity(ClientLevel level) {
        if (failed) return null;
        if (entity != null && entityLevel == level) return entity;
        entityLevel = level;
        entity = null;
        try {
            Entity e;
            if (spec.isPlayer()) {
                e = new PreviewPlayer(level, spec);
            } else {
                EntityType<?> type = EntityType.byString(spec.entityId()).orElse(null);
                e = type == null ? null : type.create(level);
            }
            if (e == null) {
                failed = true;
                return null;
            }
            applyNbt(e);
            if (spec.isPlayer() && !spec.displayName().isEmpty()) {
                e.setCustomName(Component.literal(spec.displayName()));
            }
            e.setCustomNameVisible(spec.isPlayer() && spec.showName() && !spec.displayName().isEmpty());
            entity = e;
        } catch (Exception ex) {
            QuestClustersVisuals.LOGGER.warn("Entity icon {} could not be created: {}", spec.entityId(), ex.toString());
            failed = true;
        }
        return entity;
    }

    private void applyNbt(Entity e) throws Exception {
        CompoundTag extra = new CompoundTag();
        if (!spec.nbt().isBlank()) extra.merge(TagParser.parseTag(spec.nbt()));
        for (var kv : spec.extraNbt().entrySet()) {
            extra.merge(TagParser.parseTag("{" + kv.getKey() + ":" + kv.getValue() + "}"));
        }
        if (!extra.isEmpty()) {
            CompoundTag base = e.saveWithoutId(new CompoundTag());
            base.merge(extra);
            e.load(base);
        }
    }

    /** Player model with a skin looked up by name or uuid (asynchronously; default skin until it arrives). */
    static final class PreviewPlayer extends RemotePlayer {
        private PlayerSkin skin;

        PreviewPlayer(ClientLevel level, EntityIconSpec spec) {
            super(level, new GameProfile(spec.uuid() != null ? spec.uuid() : UUID.nameUUIDFromBytes(("preview:" + spec.skin()).getBytes()),
                    spec.skin().isEmpty() ? "Player" : spec.skin()));
            CompletableFuture<java.util.Optional<GameProfile>> profile = spec.uuid() != null
                    ? SkullBlockEntity.fetchGameProfile(spec.uuid())
                    : spec.skin().isEmpty() ? CompletableFuture.completedFuture(java.util.Optional.empty()) : SkullBlockEntity.fetchGameProfile(spec.skin());
            profile.thenCompose(p -> p.map(gp -> Minecraft.getInstance().getSkinManager().getOrLoad(gp))
                            .orElse(CompletableFuture.completedFuture(null)))
                    .thenAccept(s -> skin = s);
        }

        @Override
        public PlayerSkin getSkin() {
            return skin != null ? skin : super.getSkin();
        }
    }
}
