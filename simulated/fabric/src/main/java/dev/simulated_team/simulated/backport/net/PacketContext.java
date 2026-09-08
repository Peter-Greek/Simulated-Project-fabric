package dev.simulated_team.simulated.backport.net;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Stand-in for {@code foundry.veil.api.network.handler.PacketContext}. Veil's
 * 1.20.1 line has no {@code api.network} package at all, and Simulated's 39
 * handlers call only {@code level()}, {@code player()} and one
 * {@code disconnect()}.
 */
public interface PacketContext {

    Level level();

    Player player();

    void disconnect(Component reason);
}
