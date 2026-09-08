package dev.simulated_team.simulated.network.packets.linked_typewriter;

import dev.simulated_team.simulated.network.PacketValidation;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterEntries;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.backport.net.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import dev.simulated_team.simulated.backport.net.SimCodecs;

public record TypewriterKeySavePacket(Map<Integer, LinkedTypewriterEntries.KeyboardEntry> changedKeys, BlockPos pos,
                                      boolean clearAll) implements CustomPacketPayload {

    public static final Type<TypewriterKeySavePacket> TYPE = new Type<>(Simulated.path("linked_typewriter_save"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TypewriterKeySavePacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.INT, LinkedTypewriterEntries.KeyboardEntry.STREAM_CODEC), TypewriterKeySavePacket::changedKeys,
            SimCodecs.BLOCK_POS, TypewriterKeySavePacket::pos,
            ByteBufCodecs.BOOL, TypewriterKeySavePacket::clearAll,
            TypewriterKeySavePacket::new);

    public TypewriterKeySavePacket(final LinkedTypewriterEntries keys, final BlockPos pos, final boolean clearAll) {
        this(keys.getKeyMap(), pos, clearAll);
    }


    public void handle(final ServerPacketContext context) {
        if (this.changedKeys.size() > 317 || this.changedKeys.entrySet().stream().anyMatch(e -> !PacketValidation.validKey(e.getKey()) || e.getValue().glfwKeyCode != e.getKey())) return;
        if (!PacketValidation.canInteract(context.player(), this.pos)) return;
        final Level level = context.level();

        final BlockEntity be = level.getBlockEntity(this.pos);
        if (be instanceof final LinkedTypewriterBlockEntity lbe) {
            // make sure all entries have a valid pos
            for (final LinkedTypewriterEntries.KeyboardEntry entry : this.changedKeys.values()) {
                entry.setLocation(this.pos);
            }

            lbe.getTypewriterEntries().clearAll();
            if (!this.clearAll) {
                lbe.getTypewriterEntries().addAll(this.changedKeys);
            }

            if (lbe.getTypewriterEntries().getSize() >= 26) {
                SimAdvancements.I_PAID_FOR_THE_WHOLE_TYPEWRITER.awardTo(context.player());
            }

            lbe.setChanged();
            lbe.sendData();
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
