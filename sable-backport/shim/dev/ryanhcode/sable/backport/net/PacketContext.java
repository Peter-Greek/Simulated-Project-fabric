package dev.ryanhcode.sable.backport.net;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Stand-in for {@code foundry.veil.api.network.handler.PacketContext}. Veil has
 * no {@code api.network} package at all on its 1.20.1 line, and Sable calls only
 * {@code level()} and {@code player()} on it across all 24 handlers.
 */
public interface PacketContext {

    Level level();

    Player player();
}
