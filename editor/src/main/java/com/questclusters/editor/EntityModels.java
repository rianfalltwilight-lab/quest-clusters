package com.questclusters.editor;

import com.questclusters.platform.Stacks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

/**
 * Quest "model" setting, stored the way Quest Enhance did: the quest icon is that entity's spawn egg (a barrier if
 * it has none) with {@code custom_data.quest_enhance_entity_model = "<entity id>"}, so it degrades to the egg.
 */
public final class EntityModels {
    public static final String KEY = "quest_enhance_entity_model";

    private EntityModels() {
    }

    /** Entity id stored in an icon stack, or null. */
    public static ResourceLocation modelOf(ItemStack icon) {
        if (icon == null || icon.isEmpty()) return null;
        CompoundTag t = Stacks.customData(icon);
        return t.contains(KEY, 8) ? ResourceLocation.tryParse(t.getString(KEY)) : null;
    }

    public static final String VIDEO = "quest_enhance_video", VIDEO_PLACEHOLDER = "quest_enhance_video_placeholder";

    /** Node video path stored in a quest icon, or null. */
    public static String videoOf(ItemStack icon) {
        if (icon == null || icon.isEmpty()) return null;
        String v = Stacks.customData(icon).getString(VIDEO);
        return v.isEmpty() ? null : v;
    }

    /** True for the barrier icon that only carries a node video (never drawn). */
    public static boolean isVideoPlaceholder(ItemStack icon) {
        if (icon == null || icon.isEmpty()) return false;
        return Stacks.customData(icon).getBoolean(VIDEO_PLACEHOLDER);
    }

    /** The icon with this node video ("" removes it; a placeholder icon then becomes empty again). */
    public static ItemStack withVideo(ItemStack icon, String path) {
        boolean clear = path == null || path.isEmpty();
        if (clear && videoOf(icon) == null) return icon;
        if (clear && isVideoPlaceholder(icon)) return ItemStack.EMPTY;
        ItemStack s = icon == null || icon.isEmpty() ? new ItemStack(Items.BARRIER) : icon.copy();
        CompoundTag t = Stacks.customData(s);
        if (clear) {
            t.remove(VIDEO);
            t.remove(VIDEO_PLACEHOLDER);
        } else {
            t.putString(VIDEO, path);
            if (icon == null || icon.isEmpty()) t.putBoolean(VIDEO_PLACEHOLDER, true);
        }
        Stacks.setCustomData(s, t);
        return s;
    }

    public static ItemStack iconFor(ResourceLocation entity) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entity).orElse(null);
        SpawnEggItem egg = type == null ? null : SpawnEggItem.byId(type);
        ItemStack s = new ItemStack(egg != null ? egg : Items.BARRIER);
        CompoundTag t = new CompoundTag();
        t.putString(KEY, entity.toString());
        Stacks.setCustomData(s, t);
        return s;
    }
}
