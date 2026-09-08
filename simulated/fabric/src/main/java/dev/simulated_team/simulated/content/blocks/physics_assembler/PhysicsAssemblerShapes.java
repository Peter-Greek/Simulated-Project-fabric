package dev.simulated_team.simulated.content.blocks.physics_assembler;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.20.1 copy of the upstream Physics Assembler outline/collision geometry.
 */
final class PhysicsAssemblerShapes {
    static final VoxelShaper COLLISION = VoxelShaper.forDirectional(
            combine(
                    Block.box(0, 0, 0, 16, 3, 16),
                    Block.box(2, 3, 2, 14, 12, 14)),
            Direction.NORTH);

    static final VoxelShaper CEILING_COLLISION = VoxelShaper.forDirectional(
            combine(
                    Block.box(0, 13, 0, 16, 16, 16),
                    Block.box(2, 4, 2, 14, 13, 14)),
            Direction.SOUTH);

    static final VoxelShaper WALL_COLLISION = VoxelShaper.forDirectional(
            combine(
                    Block.box(0, 0, 0, 16, 3, 16),
                    Block.box(2, 3, 2, 14, 12, 14)),
            Direction.DOWN);

    static final VoxelShaper OUTLINE = VoxelShaper.forDirectional(
            combine(
                    Block.box(0, 0, 0, 16, 4, 16),
                    Block.box(2, 3, 2, 5, 13, 14),
                    Block.box(2, 3, 2, 14, 6, 14),
                    Block.box(11, 3, 2, 14, 13, 14)),
            Direction.NORTH);

    static final VoxelShaper CEILING_OUTLINE = VoxelShaper.forDirectional(
            combine(
                    Block.box(0, 12, 0, 16, 16, 16),
                    Block.box(2, 3, 2, 5, 13, 14),
                    Block.box(2, 10, 2, 14, 13, 14),
                    Block.box(11, 3, 2, 14, 13, 14)),
            Direction.SOUTH);

    static final VoxelShaper WALL_OUTLINE = VoxelShaper.forDirectional(
            combine(
                    Block.box(0, 0, 0, 16, 4, 16),
                    Block.box(2, 3, 2, 5, 13, 14),
                    Block.box(2, 3, 2, 14, 6, 14),
                    Block.box(11, 3, 2, 14, 13, 14)),
            Direction.DOWN);

    private PhysicsAssemblerShapes() {
    }

    private static VoxelShape combine(final VoxelShape first, final VoxelShape... rest) {
        VoxelShape result = first;
        for (final VoxelShape shape : rest) {
            result = Shapes.or(result, shape);
        }
        return result;
    }
}
