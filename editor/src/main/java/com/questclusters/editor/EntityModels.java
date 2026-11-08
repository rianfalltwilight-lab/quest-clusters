package com.questclusters.editor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;

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
        CustomData data = icon.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag t = data.copyTag();
        return t.contains(KEY, 8) ? ResourceLocation.tryParse(t.getString(KEY)) : null;
    }

    public static ItemStack iconFor(ResourceLocation entity) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entity).orElse(null);
        SpawnEggItem egg = type == null ? null : SpawnEggItem.byId(type);
        ItemStack s = new ItemStack(egg != null ? egg : Items.BARRIER);
        CompoundTag t = new CompoundTag();
        t.putString(KEY, entity.toString());
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }
}
