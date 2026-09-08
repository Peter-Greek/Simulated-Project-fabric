package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.network.PacketValidation;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.util.Observable;
import dev.simulated_team.simulated.backport.net.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import dev.simulated_team.simulated.backport.net.SimCodecs;
import dev.simulated_team.simulated.util.SimReach;

public record BlockEntityObservedPacket(BlockPos pos) implements CustomPacketPayload {

    public static Type<BlockEntityObservedPacket> TYPE = new Type<>(Simulated.path("be_observed"));
    public static StreamCodec<ByteBuf, BlockEntityObservedPacket> CODEC = SimCodecs.BLOCK_POS.map(BlockEntityObservedPacket::new, BlockEntityObservedPacket::pos);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final ServerPacketContext context) {
        if (!context.level().isLoaded(this.pos)) return;
        final Level level = context.level();
        final ServerPlayer player = context.player();

        // More than 4 blocks + interaction range is way too far to observe a block
        if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(this.pos))
                > net.minecraft.util.Mth.square(SimReach.blockInteractionRange(player) + 4.0)) {
            return;
        }

        if (level.getBlockEntity(this.pos) instanceof final Observable observable) {
            observable.onObserved(player);
        }
    }
}
