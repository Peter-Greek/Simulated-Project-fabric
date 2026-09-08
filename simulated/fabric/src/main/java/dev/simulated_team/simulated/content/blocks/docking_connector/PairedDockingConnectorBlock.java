package dev.simulated_team.simulated.content.blocks.docking_connector;

import com.mojang.serialization.MapCodec;
import dev.simulated_team.simulated.index.SimBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PairedDockingConnectorBlock extends DirectionalBlock {


    private static final VoxelShape[] SHAPES = {
            box(0.0, -16.0, 0.0, 16.0, 16.0, 16.0),
            box(0.0, 0.0, 0.0, 16.0, 32.0, 16.0),
            box(0.0, 0.0, -16.0, 16.0, 16.0, 16.0),
            box(0.0, 0.0, 0.0, 16.0, 16.0, 32.0),
            box(-16.0, 0.0, 0.0, 16.0, 16.0, 16.0),
            box(0.0, 0.0, 0.0, 32.0, 16.0, 16.0),
    };

    public PairedDockingConnectorBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public @NotNull VoxelShape getShape(final @NotNull BlockState state, final @NotNull BlockGetter level, final @NotNull BlockPos pos, final @NotNull CollisionContext context) {
        return SHAPES[state.getValue(FACING).get3DDataValue()];
    }

    @Override
    public VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public @NotNull RenderShape getRenderShape(final @NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @NotNull BlockState updateShape(final BlockState state, final @NotNull Direction direction, final @NotNull BlockState neighborState, final @NotNull LevelAccessor level, final @NotNull BlockPos pos, final @NotNull BlockPos neighborPos) {
        final Direction facing = state.getValue(FACING);
        if (facing != direction) {
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }

        if (SimBlocks.DOCKING_CONNECTOR.has(neighborState) && neighborState.getValue(BlockStateProperties.FACING) == facing.getOpposite()) {
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }

        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public void playerWillDestroy(final @NotNull Level level, final @NotNull BlockPos pos, final @NotNull BlockState state, final @NotNull Player player) {
        if (!level.isClientSide()) {
            if (player.getAbilities().instabuild) {
                final BlockPos connectorPos = pos.relative(state.getValue(FACING));
                final BlockState connectorState = level.getBlockState(connectorPos);
                if (SimBlocks.DOCKING_CONNECTOR.has(connectorState)) {
                    level.setBlock(connectorPos, Blocks.AIR.defaultBlockState(), 3);
                }
            } else {
                dropResources(state, level, pos, null, player, player.getMainHandItem());
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(final @NotNull Level level, final @NotNull Player player, final @NotNull BlockPos pos, final @NotNull BlockState state, @Nullable final BlockEntity blockEntity, final @NotNull ItemStack tool) {
        super.playerDestroy(level, player, pos, Blocks.AIR.defaultBlockState(), blockEntity, tool);
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        final Direction facing = state.getValue(FACING);
        final BlockState connectorBlock = level.getBlockState(pos.relative(facing));
        return SimBlocks.DOCKING_CONNECTOR.has(connectorBlock) && connectorBlock.getValue(BlockStateProperties.FACING) == facing.getOpposite() && connectorBlock.getValue(DockingConnectorBlock.POWERED);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final @NotNull BlockPlaceContext context) {
        return null;
    }

    @Override
    public @NotNull BlockState rotate(final BlockState state, final Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(final BlockState state, final Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(final @NotNull BlockGetter level, final @NotNull BlockPos pos, final @NotNull BlockState state) {
        return SimBlocks.DOCKING_CONNECTOR.asStack();
    }

}
