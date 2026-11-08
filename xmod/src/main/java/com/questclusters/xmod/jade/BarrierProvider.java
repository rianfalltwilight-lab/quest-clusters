package com.questclusters.xmod.jade;

import dev.ftb.mods.ftbquests.block.entity.BaseBarrierBlockEntity;
import dev.ftb.mods.ftbquests.block.entity.QuestBarrierBlockEntity;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

final class BarrierProvider implements IBlockComponentProvider {
    /** Same id as FTB XMod Compat's, so the Jade config toggle carries over. */
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("ftbquests", "barrier");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public @Nullable IElement getIcon(BlockAccessor accessor, IPluginConfig config, IElement currentIcon) {
        if (accessor.getBlockEntity() instanceof BaseBarrierBlockEntity barrier && !barrier.getSkin().isEmpty()
                && !ClientQuestFile.canClientPlayerEdit()) {
            return IElementHelper.get().item(barrier.getSkin());
        }
        return null;
    }

    /** Editors only: what opens the barrier and whether it is open for them. Players just see the disguise. */
    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!ClientQuestFile.canClientPlayerEdit() || !(accessor.getBlockEntity() instanceof BaseBarrierBlockEntity barrier)) {
            return;
        }
        String target = BaseBarrierBlockEntity.BarrierSavedData.fromBlockEntity(barrier).objStr();
        Component what;
        if (barrier instanceof QuestBarrierBlockEntity) {
            long id = QuestObjectBase.parseCodeString(target);
            var object = ClientQuestFile.exists() && id != 0L ? ClientQuestFile.INSTANCE.get(id) : null;
            what = object != null ? object.getTitle() : Component.literal(target.isEmpty() ? "-" : target);
            tooltip.add(Component.translatable("quest_clusters_xmod.jade.quest", what.copy().withStyle(ChatFormatting.YELLOW)));
        } else {
            tooltip.add(Component.translatable("quest_clusters_xmod.jade.stage", Component.literal(target).withStyle(ChatFormatting.YELLOW)));
        }
        boolean open = barrier.isOpen(accessor.getPlayer());
        tooltip.add(Component.translatable(open ? "quest_clusters_xmod.jade.open" : "quest_clusters_xmod.jade.closed")
                .withStyle(open ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
}
