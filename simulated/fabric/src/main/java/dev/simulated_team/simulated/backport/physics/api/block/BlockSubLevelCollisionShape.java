package dev.simulated_team.simulated.backport.physics.api.block;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block whose physics collision shape differs from its render shape. Only the
 * physics solver reads it, so it is declared and unused until V2.
 */
public interface BlockSubLevelCollisionShape {

    VoxelShape getSubLevelCollisionShape(BlockGetter blockGetter, BlockState state);
}
