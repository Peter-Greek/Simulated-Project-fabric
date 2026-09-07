package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.network.PacketValidation;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.handle.HandleBlockEntity;
import dev.simulated_team.simulated.content.blocks.handle.ServerHandleHoldingHandler;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.backport.net.ServerPacketContext;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.simulated_team.simulated.backport.net.SimCodecs;

public record UpdatePlayerUsingHandlePacket(float desiredRange, boolean remove, BlockPos interactionPos) implements CustomPacketPayload {

    public static StreamCodec<RegistryFriendlyByteBuf, UpdatePlayerUsingHandlePacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, UpdatePlayerUsingHandlePacket::desiredRange,
            ByteBufCodecs.BOOL, UpdatePlayerUsingHandlePacket::remove,
            SimCodecs.BLOCK_POS, UpdatePlayerUsingHandlePacket::interactionPos,
            UpdatePlayerUsingHandlePacket::new);

    public static Type<UpdatePlayerUsingHandlePacket> TYPE = new Type<>(Simulated.path("update_player_handle"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final ServerPacketContext ctx) {
        if (!Float.isFinite(this.desiredRange) || this.desiredRange < -1 || this.desiredRange > 5) return;
        final ServerPlayer player = ctx.player();
        final Level level = ctx.level();

        if (this.remove) ServerHandleHoldingHandler.stopHolding(player);
        if (!PacketValidation.canInteract(player, this.interactionPos)) return;
        final BlockEntity be = level.getBlockEntity(this.interactionPos);

        if (this.remove) {
            ServerHandleHoldingHandler.stopHolding(player);
        }

        if (be instanceof final HandleBlockEntity hbe) {
            if (this.remove) {
                hbe.stopGrabbingServer(player.getUUID());
            } else {
                ServerHandleHoldingHandler.startHolding(player);
                hbe.startGrabbingServer(player.getUUID(), this.desiredRange);

                SimAdvancements.GET_A_GRIP.awardTo(player);
                if (player.fallDistance > 64) {
                    SimAdvancements.GOT_A_GRIP.awardTo(player);
                }
            }
        }
    }
}
