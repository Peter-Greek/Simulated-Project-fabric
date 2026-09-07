package dev.simulated_team.simulated.backport.physics.api.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that generates aerodynamic force for the body it rides — the
 * Symmetric Sail. Read by the aerodynamics pass, which lands in V2.
 */
public interface BlockSubLevelLiftProvider {

    float sable$getLiftScalar();

    default float sable$getParallelDragScalar() {
        return 0.0F;
    }

    /** The face the lift acts through. */
    Direction sable$getNormal(BlockState blockState);
}
