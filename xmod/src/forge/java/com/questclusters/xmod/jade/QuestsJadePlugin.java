package com.questclusters.xmod.jade;

import dev.ftb.mods.ftbquests.block.FTBQuestsBlocks;
import dev.ftb.mods.ftbquests.block.QuestBarrierBlock;
import dev.ftb.mods.ftbquests.block.StageBarrierBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade: quest and stage barriers show editors what opens them (FTB XMod Compat's Jade part for FTB Quests).
 * FTB Quests for 1.20.1 has no barrier disguises, so players see the plain barrier.
 */
@WailaPlugin
public final class QuestsJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.usePickedResult(FTBQuestsBlocks.BARRIER.get());
        registration.usePickedResult(FTBQuestsBlocks.STAGE_BARRIER.get());
        BarrierProvider provider = new BarrierProvider();
        registration.registerBlockComponent(provider, QuestBarrierBlock.class);
        registration.registerBlockComponent(provider, StageBarrierBlock.class);
    }
}
