package com.questclusters.compat.net;

import net.minecraft.resources.ResourceLocation;

/** Backport of Minecraft 1.21's {@code CustomPacketPayload}: a packet record knows its id (see platform.Net). */
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    record Type<T extends CustomPacketPayload>(ResourceLocation id) {
    }
}
