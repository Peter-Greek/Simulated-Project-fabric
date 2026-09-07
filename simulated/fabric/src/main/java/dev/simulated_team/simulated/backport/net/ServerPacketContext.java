package dev.simulated_team.simulated.backport.net;

import net.minecraft.server.level.ServerPlayer;

/** Serverbound half of {@link PacketContext}: the sender is always a real player. */
public interface ServerPacketContext extends PacketContext {

    @Override
    ServerPlayer player();
}
