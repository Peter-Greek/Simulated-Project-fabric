package dev.simulated_team.simulated.backport.physics.physics.chunk;

/** Solver-side voxel bookkeeping. Never produced here. */
public class VoxelNeighborhoodState {

    /**
     * Whether a block counts as solid to the water-occlusion pass. Sable decides
     * this from its own voxel data; without it, the block's own collision shape
     * is the honest answer, and it is what the Absorber's flood fill needs.
     */
    public static boolean isSolid(final dev.simulated_team.simulated.backport.physics.util.LevelAccelerator level,
                                  final net.minecraft.core.BlockPos pos,
                                  final net.minecraft.world.level.block.state.BlockState state) {
        return state.isCollisionShapeFullBlock(level.level(), pos);
    }
}
