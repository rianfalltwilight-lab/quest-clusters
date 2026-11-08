package com.questclusters.filters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.questclusters.filters.filter.FilterIds;
import dev.ftb.mods.ftbfiltersystem.util.FilterParser;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Locale;

/**
 * Old filter ids (Smart Filter Extras 2.x, FTB Filter System Addons 1.0.0) are read as the current ones. Done at
 * parse time rather than as extra registrations, so they never show up twice in the "Add" menu; the next save
 * writes the current id.
 */
@Mixin(value = FilterParser.class, remap = false)
public abstract class FilterParserAliasMixin {
    @WrapOperation(method = "parseFilterList", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbfiltersystem/api/FTBFilterSystemAPI;modDefaultedRL(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation quest_clusters_filters$alias(String type, Operation<ResourceLocation> op) {
        ResourceLocation alias = FilterIds.ALIASES.get(type.trim().toLowerCase(Locale.ROOT));
        return alias != null ? alias : op.call(type);
    }
}
