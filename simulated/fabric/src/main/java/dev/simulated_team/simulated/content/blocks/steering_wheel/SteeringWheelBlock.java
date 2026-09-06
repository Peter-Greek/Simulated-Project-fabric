package dev.simulated_team.simulated.content.blocks.steering_wheel;

import net.createmod.catnip.math.VoxelShaper;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Predicate;

/**
 * Fabric 1.20.1 steering wheel shell.
 *
 * The upstream wheel's animated input and analog-output block entity still
 * depend on systems being backported with Sable. For the temporary Create
 * transport this block acts as a real moving helm interaction target.
 */
public final class SteeringWheelBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty ON_FLOOR = BooleanProperty.create("on_floor");

    // Geometry copied from upstream SimBlockShapes: a mount box plus the wheel
    // itself, which sits in front of the mount on a floor wheel and behind it on
    // a ceiling wheel. Both are authored facing UP and rotated per facing.
    private static final VoxelShaper MOUNT =
            VoxelShaper.forDirectional(Block.box(2.0D, 2.0D, 0.0D, 14.0D, 12.0D, 16.0D), Direction.UP);
    private static final VoxelShaper FULL_FLOOR = VoxelShaper.forDirectional(
            Shapes.or(
                    Block.box(2.0D, 2.0D, 0.0D, 14.0D, 12.0D, 16.0D),
                    Block.box(-1.0D, 13.5D, -6.0D, 17.0D, 15.5D, 12.0D)),
            Direction.UP);
    private static final VoxelShaper FULL_CEILING = VoxelShaper.forDirectional(
            Shapes.or(
                    Block.box(2.0D, 2.0D, 0.0D, 14.0D, 12.0D, 16.0D),
                    Block.box(-1.0D, 13.5D, 4.0D, 17.0D, 15.5D, 22.0D)),
            Direction.UP);

    public SteeringWheelBlock(final Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(ON_FLOOR, true));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        // Matches upstream: clicking a side face picks floor or ceiling from
        // where the player is looking vertically, not from the clicked face.
        final boolean onFloor = switch (context.getClickedFace()) {
            case UP -> true;
            case DOWN -> false;
            default -> nearestLookingDirection(context, Direction.Axis::isVertical) == Direction.DOWN;
        };

        final Direction horizontal = nearestLookingDirection(context, Direction.Axis::isHorizontal);
        return defaultBlockState()
                .setValue(FACING, horizontal.getOpposite())
                .setValue(ON_FLOOR, onFloor);
    }

    private static Direction nearestLookingDirection(final BlockPlaceContext context,
                                                     final Predicate<Direction.Axis> axisFilter) {
        for (final Direction direction : context.getNearestLookingDirections()) {
            if (axisFilter.test(direction.getAxis())) {
                return direction;
            }
        }
        return context.getHorizontalDirection();
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
        final Direction facing = state.getValue(FACING);
        return state.getValue(ON_FLOOR) ? FULL_FLOOR.get(facing) : FULL_CEILING.get(facing);
    }

    /**
     * Upstream only ever collides against the mount, so a player can stand at the
     * helm without the wheel rim pushing them away.
     */
    @Override
    public VoxelShape getCollisionShape(final BlockState state,
                                        final BlockGetter level,
                                        final BlockPos pos,
                                        final CollisionContext context) {
        return MOUNT.get(state.getValue(FACING));
    }
}
