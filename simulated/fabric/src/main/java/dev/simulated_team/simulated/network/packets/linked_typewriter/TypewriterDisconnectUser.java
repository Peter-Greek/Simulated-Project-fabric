package dev.simulated_team.simulated.network.packets.linked_typewriter;

import dev.simulated_team.simulated.network.PacketValidation;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.backport.net.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import dev.simulated_team.simulated.backport.net.SimCodecs;

public record TypewriterDisconnectUser(BlockPos pos) implements CustomPacketPayload {

    public static Type<TypewriterDisconnectUser> TYPE = new Type<>(Simulated.path("typewriter_disconnect_user"));

    public static StreamCodec<ByteBuf, TypewriterDisconnectUser> CODEC = StreamCodec.composite(
            SimCodecs.BLOCK_POS, TypewriterDisconnectUser::pos, TypewriterDisconnectUser::new
    );

    public void handle(final ServerPacketContext context) {
        if (!context.level().isLoaded(this.pos)) return;
        if (context.level().getBlockEntity(this.pos) instanceof final LinkedTypewriterBlockEntity lbe) {
            if (lbe.checkUser(context.player().getUUID())) {
                lbe.disconnectUser();
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
