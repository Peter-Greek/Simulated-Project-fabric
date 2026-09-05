package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import dev.simulated_team.simulated.fabric.SimulatedFabricNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The moving Physics Assembler is the temporary helm for the Fabric vehicle prototype. */
public final class PhysicsAssemblerMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(final Player player,
                                           final InteractionHand activeHand,
                                           final BlockPos localPos,
                                           final AbstractContraptionEntity contraptionEntity) {
        if (activeHand != InteractionHand.MAIN_HAND || !player.getItemInHand(activeHand).isEmpty()) {
            return false;
        }

        if (!(contraptionEntity instanceof final ControlledContraptionEntity controlled)
                || !(controlled.getContraption() instanceof final PhysicsAssemblyContraption physicsAssembly)) {
            return false;
        }

        if (player.level().isClientSide) {
            return true;
        }

        final BlockPos controllerPos = physicsAssembly.getControllerPos();
        if (controllerPos == null) {
            player.displayClientMessage(Component.literal(
                    "Physics Assembler ERROR: moving assembly has no controller anchor"), false);
            return true;
        }

        final BlockEntity blockEntity = player.level().getBlockEntity(controllerPos);
        if (!(blockEntity instanceof final PhysicsAssemblerBlockEntity assembler)) {
            player.displayClientMessage(Component.literal(
                    "Physics Assembler ERROR: controller anchor is missing at "
                            + controllerPos.getX() + "," + controllerPos.getY() + "," + controllerPos.getZ()), false);
            return true;
        }

        if (!assembler.tryBeginInteraction(player.level().getGameTime(), player.getUUID())) {
            return true;
        }

        if (player.isShiftKeyDown()) {
            if (!SimulatedFabricNetworking.prepareForDisassembly(controlled)) {
                PhysicsAssemblerBlock.displayOperationResult(player,
                        PhysicsAssemblerBlockEntity.OperationResult.failure(
                                "cannot park for disassembly: snapped heading collides with the world"));
                return true;
            }
            PhysicsAssemblerBlock.displayOperationResult(player, assembler.disassembleActive());
            return true;
        }

        if (player instanceof final ServerPlayer serverPlayer) {
            SimulatedFabricNetworking.beginFlightControl(serverPlayer, controlled);
        }
        return true;
    }
}
