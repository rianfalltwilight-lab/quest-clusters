package com.questclusters.xmod.jade;

import dev.ftb.mods.ftbquests.block.QuestBarrierBlock;
import dev.ftb.mods.ftbquests.block.entity.BaseBarrierBlockEntity;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.registry.ModBlocks;
import dev.ftb.mods.ftbquests.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.ModList;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade for FTB Quests barriers. A disguised ("skinned") barrier looks like its skin to players who cannot edit
 * quests: name (FTB Quests' pick result already carries it), icon and mod name. Editors see the real block plus
 * what opens it.
 */
@WailaPlugin
public final class QuestsJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.usePickedResult(ModBlocks.BARRIER.get());
        registration.usePickedResult(ModBlocks.STAGE_BARRIER.get());
        BarrierProvider provider = new BarrierProvider();
        registration.registerBlockComponent(provider, QuestBarrierBlock.class);
        registration.registerBlockIcon(provider, QuestBarrierBlock.class);
        registration.addItemModNameCallback(stack -> {
            if (ClientQuestFile.canClientPlayerEdit()) {
                return null;
            }
            BaseBarrierBlockEntity.BarrierSavedData data = stack.get(ModDataComponents.BARRIER_SAVED.get());
            if (data == null || data.skin().isEmpty()) {
                return null;
            }
            String ns = BuiltInRegistries.ITEM.getKey(data.skin().getItem()).getNamespace();
            return ModList.get().getModContainerById(ns).map(c -> c.getModInfo().getDisplayName()).orElse(ns);
        });
    }
}
