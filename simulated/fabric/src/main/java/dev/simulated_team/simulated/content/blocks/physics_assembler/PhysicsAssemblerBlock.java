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
 * Minecraft 1.20.1 placement/state backport of the Physics Assembler block.
 *
 * The pre-Sable assembly lifecycle is testable in game: scan a glued structure,
 * build a non-mutating handoff preflight, prepare its snapshot, validate the
 * same snapshot again, detect structural/glue changes, cancel a prepared
 * assembly, persist it through reloads, and invalidate it when wrench-rotated.
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

    /**
     * Upstream PhysicsAssemblerBlock extends FaceAttachedHorizontalDirectionalBlock.
     * Reproduce its support rule explicitly on 1.20.1: the assembler must have a
     * solid support face on the same side that seeds the assembly.
     */
    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
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
        // The 1.20.1 interaction path can try an empty off-hand after the main
        // hand. Treat the assembler as a main-hand-only empty-hand control so a
        // single physical right-click cannot advance the lifecycle twice.
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

                final int interactionCount = assembler.recordInteraction();

                if (player.isShiftKeyDown()) {
                    final boolean cleared = assembler.clearPreparedAssembly();
                    player.displayClientMessage(Component.literal(cleared
                            ? "Physics Assembler: prepared assembly cleared; state=IDLE"
                            : "Physics Assembler: already IDLE"), false);
                    return InteractionResult.CONSUME;
                }

                final Direction stickyFacing = getStickyFacing(state);
                final BlockPos startPos = pos.relative(stickyFacing);
                final FabricAssemblyScanner.ScanResult scan = FabricAssemblyScanner.scan(level, pos, startPos);

                if (scan.successful()) {
                    final FabricAssemblyPlan plan = FabricAssemblyPlan.capture(level, scan);
                    final PreparedAssembly snapshot = PreparedAssembly.capture(
                            level, startPos, stickyFacing, scan, plan);
                    final PhysicsAssemblerBlockEntity.PreparationResult result = assembler.prepare(snapshot);
                    final String verb = switch (result) {
                        case PREPARED -> "PREPARED";
                        case UPDATED -> "UPDATED";
                        case VALIDATED -> "VALIDATED";
                    };
                    final String suffix = result == PhysicsAssemblerBlockEntity.PreparationResult.VALIDATED
                            ? "; stable validations=" + assembler.getStableValidationCount()
                                    + "; dry-run handoff is stable"
                            : "; interact again without changing the structure to validate";
                    final String warning = plan.hasDeferredCreateContraptions()
                            ? "; WARNING: intersecting moving Create contraption(s) still need upstream expansion logic"
                            : "";

                    player.displayClientMessage(Component.literal(
                            "Physics Assembler " + verb + ": " + snapshot.blockCount()
                                    + " block(s), sticky=" + snapshot.stickyFacing()
                                    + ", seed=" + shortPos(snapshot.startPos())
                                    + ", bounds " + shortPos(snapshot.min())
                                    + " -> " + shortPos(snapshot.max())
                                    + ", size=" + snapshot.dimensions()
                                    + "; preflight: " + plan.summary()
                                    + "; signature=" + Long.toUnsignedString(snapshot.signature(), 16)
                                    + suffix + warning
                                    + "; interaction=" + interactionCount), false);
                } else {
                    assembler.recordFailedScan(scan.error());
                    player.displayClientMessage(Component.literal(
                            "Physics Assembler ERROR: " + scan.error()
                                    + " at " + shortPos(scan.problemPos())
                                    + "; prepared snapshot cleared"), false);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Create's default IWrenchable implementation looks for Create's six-way
     * FACING property. Physics Assembler instead uses the vanilla
     * horizontal-facing + attach-face pair, so keep the attach face fixed and
     * rotate the horizontal orientation consistently.
     */
    @Override
    public BlockState getRotatedBlockState(final BlockState originalState, final Direction targetedFace) {
        return originalState.setValue(FACING, originalState.getValue(FACING).getClockWise());
    }

    @Override
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
        final InteractionResult result = IWrenchable.super.onWrenched(state, context);
        if (!context.getLevel().isClientSide && result.consumesAction()) {
            final BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
            if (blockEntity instanceof final PhysicsAssemblerBlockEntity assembler) {
                assembler.clearPreparedAssembly();
            }
        }
        return result;
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

    private static String shortPos(final BlockPos pos) {
        if (pos == null) {
            return "?";
        }
        return "[" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "]";
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
