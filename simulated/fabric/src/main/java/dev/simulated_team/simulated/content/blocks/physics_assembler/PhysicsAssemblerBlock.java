package dev.simulated_team.simulated.content.blocks.physics_assembler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Minecraft 1.20.1 placement/state backport of the Physics Assembler block.
 *
 * The block entity is now present and persistent. Sable assembly hooks, Create
 * deployer behavior and the original hold interaction remain deferred until
 * their supporting 1.20.1 layers are available.
 */
public final class PhysicsAssemblerBlock extends Block implements EntityBlock {
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
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new PhysicsAssemblerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos,
                                 final Player player, final InteractionHand hand, final BlockHitResult hit) {
        if (!level.isClientSide) {
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof final PhysicsAssemblerBlockEntity assembler) {
                final int count = assembler.recordInteraction();
                player.displayClientMessage(Component.literal(
                        "Physics Assembler Fabric block entity active; persistent interaction count=" + count), false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
