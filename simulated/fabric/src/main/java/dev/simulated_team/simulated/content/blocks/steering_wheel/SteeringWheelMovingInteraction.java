package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblyContraption;
import dev.simulated_team.simulated.fabric.SimulatedFabricNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** Starts temporary vehicle control from a steering wheel on a moving assembly. */
public final class SteeringWheelMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(final Player player,
                                           final InteractionHand activeHand,
                                           final BlockPos localPos,
                                           final AbstractContraptionEntity contraptionEntity) {
        if (activeHand != InteractionHand.MAIN_HAND) {
            return false;
        }

        if (!(contraptionEntity instanceof final ControlledContraptionEntity controlled)
                || !(controlled.getContraption() instanceof PhysicsAssemblyContraption)) {
            return false;
        }

        if (player.level().isClientSide) {
            return true;
        }

        if (player instanceof final ServerPlayer serverPlayer) {
            // Sit one block above the moving wheel. The seat is logical only and
            // is inserted into Create's contraption seat list on demand.
            SimulatedFabricNetworking.beginFlightControl(serverPlayer, controlled, localPos.above());
        }
        return true;
    }
}
