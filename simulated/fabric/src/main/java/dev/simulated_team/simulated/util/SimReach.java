package dev.simulated_team.simulated.util;

import net.minecraft.world.entity.player.Player;

/**
 * How far a player can reach to interact with a block.
 *
 * <p>1.20.5 made reach an entity attribute,
 * {@code Attributes.BLOCK_INTERACTION_RANGE}, and upstream reads it. On 1.20.1
 * reach is not an attribute at all — it is a constant in
 * {@code MultiPlayerGameMode}, 5 blocks in creative and 4.5 otherwise, and no
 * Fabric API on this stack exposes a modifiable version of it.
 *
 * <p>So the vanilla constants are used directly. A pack that changes reach
 * through some other mod will not be reflected here; that is the deviation, and
 * it goes away whenever the port moves to a version with the attribute.
 */
public final class SimReach {

    public static final double CREATIVE_BLOCK_REACH = 5.0;

    public static final double SURVIVAL_BLOCK_REACH = 4.5;

    private SimReach() {
    }

    public static double blockInteractionRange(final Player player) {
        return player.isCreative() ? CREATIVE_BLOCK_REACH : SURVIVAL_BLOCK_REACH;
    }
}
