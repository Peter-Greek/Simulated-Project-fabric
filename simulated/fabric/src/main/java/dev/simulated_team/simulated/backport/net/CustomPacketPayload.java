package dev.simulated_team.simulated.backport.net;

import net.minecraft.resources.ResourceLocation;

/**
 * Stand-in for {@code net.minecraft.network.protocol.common.custom.CustomPacketPayload}
 * (1.20.2+). Sable uses only the nested {@code Type} holder and {@code type()},
 * so on 1.20.1 the id is simply the plugin-message channel the payload travels on.
 */
public interface CustomPacketPayload {

    Type<? extends CustomPacketPayload> type();

    record Type<T extends CustomPacketPayload>(ResourceLocation id) {
    }
}
