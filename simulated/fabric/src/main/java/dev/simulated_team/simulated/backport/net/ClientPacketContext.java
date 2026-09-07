package dev.simulated_team.simulated.backport.net;

import net.minecraft.client.player.LocalPlayer;

/** Clientbound half of {@link PacketContext}: the receiver is the local player. */
public interface ClientPacketContext extends PacketContext {

    @Override
    LocalPlayer player();
}
