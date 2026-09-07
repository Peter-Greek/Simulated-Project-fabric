package dev.simulated_team.simulated.content.blocks.steering_wheel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Carries the wheel's rendered state.
 *
 * <p>Upstream's block entity is a {@code KineticBlockEntity} that drives the
 * wheel's rotation, analog output and stress reporting. None of that is here
 * yet — this exists so {@link SteeringWheelRenderer} has something to hang off,
 * because the wheel rim is a separate model part and cannot come from the
 * blockstate. The kinetic behaviour lands with V1 §1.4.
 */
public class SteeringWheelBlockEntity extends BlockEntity {

    public SteeringWheelBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    /**
     * The wheel is wider than its block and sits proud of it, so the rim would
     * otherwise be culled while its own block is still on screen.
     */
    public float getRenderAngle(final float partialTicks) {
        return 0.0F;
    }
}
