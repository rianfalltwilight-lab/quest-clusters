package com.questclusters.platform;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.function.UnaryOperator;

/** What a mod's constructor gets from the loader (Forge 47: read from the loading context). */
public record ModContext(IEventBus modBus, String modId) {
    /** For the mod being constructed right now. */
    @SuppressWarnings("removal")
    public static ModContext current() {
        return new ModContext(FMLJavaModLoadingContext.get().getModEventBus(), ModLoadingContext.get().getActiveNamespace());
    }

    @SuppressWarnings("removal")
    public void clientConfig(ForgeConfigSpec spec) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, spec);
    }

    @SuppressWarnings("removal")
    public void commonConfig(ForgeConfigSpec spec) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, spec);
    }

    /** The settings screen opened from the mods list; client only. */
    @SuppressWarnings("removal")
    public void configScreen(UnaryOperator<Screen> factory) {
        ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> factory.apply(parent)));
    }
    /** The settings screen another mod registered for the mods list, if any; client only. */
    public static java.util.Optional<UnaryOperator<Screen>> configScreenOf(String modId) {
        return net.minecraftforge.fml.ModList.get().getModContainerById(modId).flatMap(c -> c.getCustomExtension(
                        net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class)
                .map(f -> (UnaryOperator<Screen>) parent -> f.screenFunction().apply(net.minecraft.client.Minecraft.getInstance(), parent)));
    }
}
