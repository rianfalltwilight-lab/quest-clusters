package com.questclusters.platform;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.function.UnaryOperator;

/** What a mod's constructor gets from the loader (NeoForge: injected into the constructor). */
public record ModContext(IEventBus modBus, ModContainer container) {
    public String modId() {
        return container.getModId();
    }

    public void clientConfig(ModConfigSpec spec) {
        container.registerConfig(ModConfig.Type.CLIENT, spec);
    }

    public void commonConfig(ModConfigSpec spec) {
        container.registerConfig(ModConfig.Type.COMMON, spec);
    }

    /** The settings screen opened from the mods list; client only. */
    public void configScreen(UnaryOperator<Screen> factory) {
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (net.neoforged.neoforge.client.gui.IConfigScreenFactory) (mod, parent) -> factory.apply(parent));
    }
    /** The settings screen another mod registered for the mods list, if any; client only. */
    public static java.util.Optional<UnaryOperator<Screen>> configScreenOf(String modId) {
        return net.neoforged.fml.ModList.get().getModContainerById(modId).flatMap(c -> c.getCustomExtension(
                        net.neoforged.neoforge.client.gui.IConfigScreenFactory.class)
                .map(f -> (UnaryOperator<Screen>) parent -> f.createScreen(c, parent)));
    }
}
