package com.questclusters.xmod.jade;

import com.questclusters.platform.Rl;
import dev.ftb.mods.ftbquests.block.entity.BarrierBlockEntity;
import dev.ftb.mods.ftbquests.block.entity.QuestBarrierBlockEntity;
import dev.ftb.mods.ftbquests.block.entity.StageBarrierBlockEntity;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

final class BarrierProvider implements IBlockComponentProvider {
    /** Same id as FTB XMod Compat's, so the Jade config toggle carries over. */
    private static final ResourceLocation UID = Rl.of("ftbquests", "barrier");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    /** Editors only: what opens the barrier and whether it is open for them. */
    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!ClientQuestFile.canClientPlayerEdit() || !(accessor.getBlockEntity() instanceof BarrierBlockEntity barrier)) {
            return;
        }
        var tag = accessor.getBlockEntity().saveWithoutMetadata();
        if (barrier instanceof QuestBarrierBlockEntity) {
            long id = tag.getLong("Object");
            var object = ClientQuestFile.exists() && id != 0L ? ClientQuestFile.INSTANCE.get(id) : null;
            Component what = object != null ? object.getTitle() : Component.literal(id == 0L ? "-" : QuestObjectBase.getCodeString(id));
            tooltip.add(Component.translatable("quest_clusters_xmod.jade.quest", what.copy().withStyle(ChatFormatting.YELLOW)));
        } else if (barrier instanceof StageBarrierBlockEntity) {
            tooltip.add(Component.translatable("quest_clusters_xmod.jade.stage", Component.literal(tag.getString("Stage")).withStyle(ChatFormatting.YELLOW)));
        }
        boolean open = barrier.isOpen(accessor.getPlayer());
        tooltip.add(Component.translatable(open ? "quest_clusters_xmod.jade.open" : "quest_clusters_xmod.jade.closed")
                .withStyle(open ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
}
