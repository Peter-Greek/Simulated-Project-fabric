package dev.simulated_team.simulated.util;

import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.backport.world.ItemInteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Function;

/**
 * Runs a block's item-use pass against its block entity.
 *
 * <p>Create grew {@code IBE#onBlockEntityUseItemOn} when 1.20.5 split block
 * interaction in two. Its 1.20.1 build has only {@code onBlockEntityUse}, which
 * returns an {@code InteractionResult}, so this adapts between the two: the
 * upstream lambda still returns an {@link ItemInteractionResult}, and the
 * result is converted on the way out.
 *
 * <p>Ported blocks call this instead of {@code this.onBlockEntityUseItemOn}; the
 * bridge in {@code tools/bridge_block_use.py} is what routes vanilla's single
 * {@code use} into the item pass in the first place.
 */
public final class SimBlockEntityUse {

    private SimBlockEntityUse() {
    }

    public static <T extends BlockEntity> ItemInteractionResult onBlockEntityUseItemOn(
            final IBE<T> block, final BlockGetter level, final BlockPos pos,
            final Function<T, ItemInteractionResult> action) {
        final java.util.Optional<T> blockEntity = block.getBlockEntityOptional(level, pos);
        if (blockEntity.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return action.apply(blockEntity.get());
    }
}
