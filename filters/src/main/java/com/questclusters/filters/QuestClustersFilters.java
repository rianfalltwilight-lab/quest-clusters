package com.questclusters.filters;

import dev.ftb.mods.ftbfiltersystem.api.event.FilterRegistrationEvent;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quest Clusters Filters: extra FTB Filter System smart filters (item list, several item tags, block tags, fuel
 * value), replacing Smart Filter Extras with the same filter strings.
 */
@Mod(QuestClustersFilters.MOD_ID)
public final class QuestClustersFilters {
    public static final String MOD_ID = "quest_clusters_filters";
    public static final Logger LOGGER = LoggerFactory.getLogger("Quest Clusters Filters");
    /** Raised whenever a filter form appears that older versions cannot read. */
    public static final String PROTOCOL = "1";

    /**
     * Never sent: registering it makes the channel required, so NeoForge refuses a connection where only one side
     * has the module (otherwise the client could not open these filters, or edits would be dropped by the server).
     */
    public record Presence() implements CustomPacketPayload {
        public static final Type<Presence> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "presence"));
        public static final StreamCodec<ByteBuf, Presence> CODEC = StreamCodec.unit(new Presence());

        @Override
        public Type<Presence> type() {
            return TYPE;
        }
    }

    public QuestClustersFilters(IEventBus modBus) {
        if (!net.neoforged.fml.ModList.get().isLoaded("ftbfiltersystem")) {
            LOGGER.info("FTB Filter System is not installed: Quest Clusters Filters stays inactive");
            return;
        }
        Setup.run(modBus);
    }

    /** Separate class so nothing from FTB Filter System is loaded when it is absent. */
    private static final class Setup {
        static void run(IEventBus modBus) {
        FilterRegistrationEvent.REGISTER.register(FilterRegistration::register);
        modBus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar(PROTOCOL).playToClient(Presence.TYPE, Presence.CODEC, (p, c) -> {
        }));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> FilterCommands.register(e.getDispatcher()));
        if (FMLEnvironment.dist.isClient()) {
            com.questclusters.filters.client.FiltersClient.init(modBus);
        }
        }
    }
}
