package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Minecraft 1.20.1 placement/state backport of the Physics Assembler block.
 *
 * The block now has its upstream geometry, Create wrench integration, a
 * persistent block entity and a Create-aware structure discovery pass. Moving
 * the discovered blocks into a Sable sub-level is the next stage.
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
        if (!player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof final PhysicsAssemblerBlockEntity assembler) {
                final int interactionCount = assembler.recordInteraction();
                final Direction stickyFacing = getStickyFacing(state);
                final BlockPos startPos = pos.relative(stickyFacing);
                final FabricAssemblyScanner.ScanResult scan = FabricAssemblyScanner.scan(level, pos, startPos);

                if (scan.successful()) {
                    assembler.recordSuccessfulScan(scan.blocks().size());
                    player.displayClientMessage(Component.literal(
                            "Physics Assembler probe: " + scan.blocks().size()
                                    + " block(s), bounds " + shortPos(scan.min())
                                    + " -> " + shortPos(scan.max())
                                    + "; interaction=" + interactionCount), false);
                } else {
                    assembler.recordFailedScan();
                    player.displayClientMessage(Component.literal(
                            "Physics Assembler probe failed: " + scan.error()
                                    + " at " + shortPos(scan.problemPos())), false);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
