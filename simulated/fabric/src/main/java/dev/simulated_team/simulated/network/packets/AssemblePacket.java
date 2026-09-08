package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.network.PacketValidation;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.index.SimStats;
import dev.simulated_team.simulated.backport.net.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.simulated_team.simulated.backport.net.SimCodecs;

public record AssemblePacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<AssemblePacket> TYPE = new Type<>(Simulated.path("assemble"));

    public static final StreamCodec<ByteBuf, AssemblePacket> CODEC = StreamCodec.composite(
            SimCodecs.BLOCK_POS, packet -> packet.pos,
            AssemblePacket::new);

    @Override
    public Type<AssemblePacket> type() {
        return TYPE;
    }

    public void handle(final ServerPacketContext context) {
        if (!PacketValidation.canInteract(context.player(), this.pos)) return;
        final ServerLevel level = (ServerLevel) context.player().level();

        final BlockEntity blockEntity = level.getBlockEntity(this.pos);

        if (blockEntity instanceof final PhysicsAssemblerBlockEntity assembler) {
            assembler.toggleAssembly();
            SimStats.INTERACT_WITH_ASSEMBLER.awardTo(context.player());
        }
    }
}
