package dev.simulated_team.simulated.backport.physics.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that wants to know when it is moved into or out of a sub-level, so it
 * can carry its state across. Nothing calls these on this stack — assembly into
 * a sub-level is V2 — so a block that implements it keeps the hook and it stays
 * unfired.
 */
public interface BlockSubLevelAssemblyListener {

    default void beforeMove(ServerLevel originLevel, ServerLevel resultingLevel,
                            BlockState newState, BlockPos oldPos, BlockPos newPos) {
    }

    default void afterMove(ServerLevel originLevel, ServerLevel resultingLevel,
                           BlockState newState, BlockPos oldPos, BlockPos newPos) {
    }
}
