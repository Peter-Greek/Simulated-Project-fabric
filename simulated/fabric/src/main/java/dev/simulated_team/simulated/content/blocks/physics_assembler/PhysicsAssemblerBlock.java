package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Fabric 1.20.1 Physics Assembler.
 *
 * Normal empty-hand use toggles a real moving Create contraption instead of a
 * dry-run validation state. Sneak-use nudges an active assembly one block in
 * the direction the player is looking; this is temporary transport control
 * until Sable supplies rigid-body physics on 1.20.1.
 */
public final class PhysicsAssemblerBlock extends Block implements EntityBlock, IWrenchable {
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

        final BlockState state = defaultBlockState()
                .setValue(FACE, attachFace)
                .setValue(FACING, facing);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof final PhysicsAssemblerBlockEntity assembler
                && assembler.isHoldingAssembly()) {
            return true;
        }

        final Direction supportDirection = getStickyFacing(state);
        final BlockPos supportPos = pos.relative(supportDirection);
        return !level.getBlockState(supportPos)
                .getBlockSupportShape(level, supportPos)
                .getFaceShape(supportDirection.getOpposite())
                .isEmpty();
    }

    @Override
    public BlockState updateShape(final BlockState state,
                                  final Direction direction,
                                  final BlockState neighborState,
                                  final LevelAccessor level,
                                  final BlockPos pos,
                                  final BlockPos neighborPos) {
        if (direction == getStickyFacing(state) && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new PhysicsAssemblerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos,
                                 final Player player, final InteractionHand hand, final BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof final PhysicsAssemblerBlockEntity assembler) {
                if (!assembler.tryBeginInteraction(level.getGameTime(), player.getUUID())) {
                    return InteractionResult.CONSUME;
                }

                final PhysicsAssemblerBlockEntity.OperationResult result = player.isShiftKeyDown()
                        ? assembler.nudgeAssembly(nudgeDirection(player))
                        : assembler.toggleAssembly();
                displayOperationResult(player, result);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static Direction nudgeDirection(final Player player) {
        final float pitch = player.getXRot();
        if (pitch <= -50.0F) {
            return Direction.UP;
        }
        if (pitch >= 50.0F) {
            return Direction.DOWN;
        }
        return player.getDirection();
    }

    public static void displayOperationResult(final Player player,
                                              final PhysicsAssemblerBlockEntity.OperationResult result) {
        final String prefix = result.successful() ? "Physics Assembler: " : "Physics Assembler ERROR: ";
        final String count = result.blockCount() > 0 ? " [" + result.blockCount() + " blocks]" : "";
        player.displayClientMessage(Component.literal(prefix + result.message() + count), false);
    }

    @Override
    public BlockState getRotatedBlockState(final BlockState originalState, final Direction targetedFace) {
        return IWrenchable.super.getRotatedBlockState(originalState, targetedFace);
    }

    @Override
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof final PhysicsAssemblerBlockEntity assembler
                && assembler.isHoldingAssembly()) {
            if (!context.getLevel().isClientSide && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.literal(
                        "Physics Assembler: disassemble the active structure before wrenching the controller."), true);
            }
            return InteractionResult.SUCCESS;
        }

        final BlockState rotated = getRotatedBlockState(state, context.getClickedFace());
        final boolean wallOrientationLocked = state.getValue(FACE) == AttachFace.WALL && rotated == state;
        final InteractionResult result = IWrenchable.super.onWrenched(state, context);

        if (!context.getLevel().isClientSide && wallOrientationLocked && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(Component.literal(
                    "Physics Assembler: wall orientation is support-locked; floor/ceiling mounts can rotate."), true);
        }
        return result;
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos,
                         final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof final PhysicsAssemblerBlockEntity assembler && assembler.hasActiveAssembly()) {
                assembler.disassembleActive(false);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                               final CollisionContext context) {
        final Direction facing = state.getValue(FACING);
        return switch (state.getValue(FACE)) {
            case CEILING -> PhysicsAssemblerShapes.CEILING_OUTLINE.get(facing);
            case FLOOR -> PhysicsAssemblerShapes.OUTLINE.get(facing);
            case WALL -> PhysicsAssemblerShapes.WALL_OUTLINE.get(facing.getOpposite());
        };
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                        final CollisionContext context) {
        final Direction facing = state.getValue(FACING);
        return switch (state.getValue(FACE)) {
            case CEILING -> PhysicsAssemblerShapes.CEILING_COLLISION.get(facing);
            case FLOOR -> PhysicsAssemblerShapes.COLLISION.get(facing);
            case WALL -> PhysicsAssemblerShapes.WALL_COLLISION.get(facing.getOpposite());
        };
    }

    public static Direction getStickyFacing(final BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
            case WALL -> state.getValue(FACING).getOpposite();
        };
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
