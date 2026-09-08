package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.network.PacketValidation;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import dev.simulated_team.simulated.util.hold_interaction.BlockHoldInteraction;
import dev.simulated_team.simulated.backport.net.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.simulated_team.simulated.backport.net.SimCodecs;

public record ThrottleLeverSignalPacket(BlockPos pos, int signal) implements CustomPacketPayload {
    public static final Type<ThrottleLeverSignalPacket> TYPE = new Type<>(Simulated.path("throttle_lever_signal"));
    public static final StreamCodec<ByteBuf, ThrottleLeverSignalPacket> CODEC = StreamCodec.composite(
            SimCodecs.BLOCK_POS, ThrottleLeverSignalPacket::pos,
            ByteBufCodecs.INT, ThrottleLeverSignalPacket::signal,
            ThrottleLeverSignalPacket::new
    );

    @Override
    public Type<ThrottleLeverSignalPacket> type() {
        return TYPE;
    }

    public void handle(final ServerPacketContext context) {
        if (this.signal < 0 || this.signal > 15) return;
        if (!PacketValidation.canInteract(context.player(), this.pos)) return;
        final ServerPlayer player = context.player();
        final ServerLevel level = (ServerLevel) player.level();

        final BlockEntity blockEntity = level.getBlockEntity(this.pos);

        if (blockEntity instanceof final ThrottleLeverBlockEntity throttleLever) {
            if (!BlockHoldInteraction.inInteractionRange(player, this.pos.getCenter(), 1)) return;

            throttleLever.setSignal(this.signal);
        }
    }
}
