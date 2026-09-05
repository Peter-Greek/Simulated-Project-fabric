package dev.simulated_team.simulated.content.blocks.steering_wheel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Fabric 1.20.1 steering wheel shell.
 *
 * The upstream wheel's animated input and analog-output block entity still
 * depend on systems being backported with Sable. For the temporary Create
 * transport this block acts as a real moving helm interaction target.
 */
public final class SteeringWheelBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty ON_FLOOR = BooleanProperty.create("on_floor");

    private static final VoxelShape FLOOR_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private static final VoxelShape CEILING_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public SteeringWheelBlock(final Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(ON_FLOOR, true));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final boolean onFloor = context.getClickedFace() != Direction.DOWN;
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(ON_FLOOR, onFloor);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ON_FLOOR);
    }

    @Override
    public VoxelShape getShape(final BlockState state,
                               final BlockGetter level,
                               final BlockPos pos,
                               final CollisionContext context) {
        return state.getValue(ON_FLOOR) ? FLOOR_SHAPE : CEILING_SHAPE;
    }
}
