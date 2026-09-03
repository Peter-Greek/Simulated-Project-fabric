package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Lets the visible Physics Assembler remain the interaction point while it is
 * being rendered as part of a Create contraption. The real controller is the
 * invisible anchor left at the assembly's original world position.
 */
public final class PhysicsAssemblerMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(final Player player,
                                           final InteractionHand activeHand,
                                           final BlockPos localPos,
                                           final AbstractContraptionEntity contraptionEntity) {
        if (activeHand != InteractionHand.MAIN_HAND || !player.getItemInHand(activeHand).isEmpty()) {
            return false;
        }

        if (!(contraptionEntity.getContraption() instanceof final PhysicsAssemblyContraption physicsAssembly)) {
            return false;
        }

        // Interaction packets are authoritative server-side. Returning true on
        // the client prevents the click from falling through to blocks behind
        // the moving assembler while the server performs the operation.
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

        final PhysicsAssemblerBlockEntity.OperationResult result = player.isShiftKeyDown()
                ? assembler.nudgeAssembly(PhysicsAssemblerBlock.nudgeDirection(player))
                : assembler.disassembleActive();
        PhysicsAssemblerBlock.displayOperationResult(player, result);
        return true;
    }
}
