package com.questclusters.editor.mixin;

import com.questclusters.editor.EntityModels;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.Quest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Quest property "Model": shows an entity instead of the icon. Written into the quest icon (see EntityModels). */
@Mixin(value = Quest.class, remap = false)
public abstract class QuestModelConfigMixin {
    @Inject(method = "fillConfigGroup", at = @At("TAIL"))
    private void quest_clusters_editor$modelField(ConfigGroup config, CallbackInfo ci) {
        Quest self = (Quest) (Object) this;
        ItemStack raw = ((QuestObjectIconAccessor) (Object) self).quest_clusters_editor$rawIcon();
        ResourceLocation current = EntityModels.modelOf(raw);
        List<String> ids = new ArrayList<>();
        ids.add("");
        BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(ResourceLocation::toString).sorted().forEach(ids::add);
        NameMap<String> names = NameMap.of("", ids).name(s -> s.isEmpty()
                ? Component.translatable("quest_clusters_editor.model.none")
                : Component.translatable("entity." + s.replace(':', '.'))).create();
        config.addEnum("entity_model", current == null ? "" : current.toString(), v -> {
            ResourceLocation now = EntityModels.modelOf(((QuestObjectIconAccessor) (Object) self).quest_clusters_editor$rawIcon());
            if (v.isEmpty()) {
                if (now != null) self.setRawIcon(ItemStack.EMPTY);
            } else if (now == null || !now.toString().equals(v)) {
                self.setRawIcon(EntityModels.iconFor(ResourceLocation.parse(v)));
            }
        }, names).setOrder(-125);
    }
}
