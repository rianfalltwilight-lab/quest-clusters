package com.questclusters.filters;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.ftb.mods.ftbfiltersystem.api.FTBFilterSystemAPI;
import dev.ftb.mods.ftbfiltersystem.registry.item.SmartFilterItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /qcfilters downgrade} (also {@code /smart_filter_extras downgrade}): converts the smart filter in the main
 * hand to FTB Filter System's own filters where possible, before removing this module.
 */
public final class FilterCommands {
    private FilterCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        for (String root : new String[]{"qcfilters", "smart_filter_extras"}) {
            d.register(Commands.literal(root).then(Commands.literal("downgrade").executes(FilterCommands::downgrade)));
        }
    }

    private static int downgrade(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (!FTBFilterSystemAPI.api().isFilterItem(held)) {
            ctx.getSource().sendFailure(Component.translatable("quest_clusters_filters.downgrade.not_filter"));
            return 0;
        }
        var registries = ctx.getSource().registryAccess();
        String before = SmartFilterItem.getFilterString(held);
        Downgrade.Report report = new Downgrade.Report();
        String after;
        try {
            after = Downgrade.rewrite(SmartFilterItem.getFilter(held, registries), registries, report);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("quest_clusters_filters.downgrade.invalid", e.getMessage()));
            return 0;
        }
        if (after.length() > Downgrade.MAX_LENGTH) {
            ctx.getSource().sendFailure(Component.translatable("quest_clusters_filters.downgrade.too_long", after.length(), Downgrade.MAX_LENGTH));
            return 0;
        }
        if (!after.equals(before)) SmartFilterItem.setFilter(held, after);
        ctx.getSource().sendSuccess(() -> Component.translatable("quest_clusters_filters.downgrade.done",
                report.converted, report.droppedMissing, report.keptNeedsModule), false);
        if (report.unconvertibleVariants > 0) {
            ctx.getSource().sendSuccess(() -> Component.translatable("quest_clusters_filters.downgrade.variants", report.unconvertibleVariants), false);
        }
        return 1;
    }
}
