package dev.simulated_team.simulated.content.blocks.physics_assembler;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Minecraft 1.20.1 placement/state backport of the Physics Assembler block.
 *
 * The upstream block also owns Sable assembly hooks, its block entity, Create
 * deployer interaction and hold interaction. Those are intentionally deferred
 * until the Sable and networking layers are available on Fabric 1.20.1.
 */
public final class PhysicsAssemblerBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

    public PhysicsAssemblerBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.FLOOR));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction clickedFace = context.getClickedFace();
        final AttachFace attachFace;
        final Direction facing;

        if (clickedFace == Direction.UP) {
            attachFace = AttachFace.FLOOR;
            facing = context.getHorizontalDirection();
        } else if (clickedFace == Direction.DOWN) {
            attachFace = AttachFace.CEILING;
            facing = context.getHorizontalDirection();
        } else {
            attachFace = AttachFace.WALL;
            facing = clickedFace;
        }

        return defaultBlockState()
                .setValue(FACE, attachFace)
                .setValue(FACING, facing);
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }
}
