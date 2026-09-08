package dev.simulated_team.simulated.content.blocks.analog_transmission;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import dev.simulated_team.simulated.util.placement_helpers.CogwheelPlacementExtension;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import dev.simulated_team.simulated.backport.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionResult;

/**
 * The parent Block. Implements {@link ExtraKinetics.ExtraKineticsBlock ExtraKienticsBlock} to ensure {@link com.simibubi.create.content.kinetics.RotationPropagator} sees the ExtraKinetic BlockEntity
 */
public class AnalogTransmissionBlock extends RotatedPillarKineticBlock implements IBE<AnalogTransmissionBlockEntity>, ExtraKinetics.ExtraKineticsBlock {

    public static final int placementHelperId = PlacementHelpers.register(new CogwheelPlacementExtension((i) -> i.getItem() instanceof CogwheelBlockItem, SimBlocks.ANALOG_TRANSMISSION::has));

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public AnalogTransmissionBlock(final Properties properties) {
        super(properties);
    }

    protected ItemInteractionResult useItemOn(final ItemStack itemStack, final BlockState blockState, final Level level, final BlockPos blockPos, final Player player, final InteractionHand interactionHand, final BlockHitResult blockHitResult) {
        final ItemStack heldItem = player.getItemInHand(interactionHand);

        final IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
        if (helper.matchesItem(heldItem)) {
            return ItemInteractionResult.from(helper
                    .getOffset(player, level, blockState, blockPos, blockHitResult)
                    .placeInWorld(level, (BlockItem) heldItem.getItem(), player, interactionHand, blockHitResult));
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(POWERED));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(POWERED, context.getLevel()
                .hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public IRotate getExtraKineticsRotationConfiguration() {
        return AnalogTransmissionBlockEntity.AnalogTransmissionCogwheel.EXTRA_COGWHEEL_CONFIG;
    }

    @Override
    public Direction.Axis getRotationAxis(final BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state, final Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Class<AnalogTransmissionBlockEntity> getBlockEntityClass() {
        return AnalogTransmissionBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AnalogTransmissionBlockEntity> getBlockEntityType() {
        return SimBlockEntityTypes.SIMPLE_BE.get();
    }

    // 1.20.1 has one block-use method, so this dispatches into
    // upstream's item pass. An empty hand, or an item pass that declined, falls
    // through to the superclass as vanilla does.
    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos,
                                 final Player player, final InteractionHand hand,
                                 final BlockHitResult hitResult) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty()) {
            final ItemInteractionResult itemResult =
                    this.useItemOn(stack, state, level, pos, player, hand, hitResult);
            if (!itemResult.shouldRunDefault()) {
                return itemResult.result();
            }
        }

        return super.use(state, level, pos, player, hand, hitResult);
    }

}