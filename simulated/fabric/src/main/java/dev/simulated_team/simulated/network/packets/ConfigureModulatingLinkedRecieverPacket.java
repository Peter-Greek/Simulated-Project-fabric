package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.network.packets.helpers.SimBlockEntityConfigurationPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import dev.simulated_team.simulated.backport.net.SimCodecs;

public class ConfigureModulatingLinkedRecieverPacket extends SimBlockEntityConfigurationPacket<ModulatingLinkedReceiverBlockEntity> {
    public static final Type<ConfigureModulatingLinkedRecieverPacket> TYPE = new Type<>(Simulated.path("configure_modulating_linked_reciever"));
    public static final StreamCodec<ByteBuf, ConfigureModulatingLinkedRecieverPacket> CODEC = StreamCodec.composite(
            SimCodecs.BLOCK_POS, SimBlockEntityConfigurationPacket::getPos,
            ByteBufCodecs.INT, ConfigureModulatingLinkedRecieverPacket::getMinRange,
            ByteBufCodecs.INT, ConfigureModulatingLinkedRecieverPacket::getMaxRange,
            ConfigureModulatingLinkedRecieverPacket::new);

    private final int minRange;
    private final int maxRange;

    public ConfigureModulatingLinkedRecieverPacket(final BlockPos pos, final int minRange, final int maxRange) {
        super(pos, ModulatingLinkedReceiverBlockEntity.class);
        this.minRange = minRange;
        this.maxRange = maxRange;
    }

    public int getMinRange() {
        return this.minRange;
    }

    public int getMaxRange() {
        return this.maxRange;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    protected void applySettings(final ServerPlayer serverPlayer, final ModulatingLinkedReceiverBlockEntity be) {
        {
            if (this.minRange < 1 || this.maxRange > 256 || this.minRange > this.maxRange) return;
            final ModulatingLinkedReceiverBlockEntity abe = be;
            abe.minRange = this.minRange;
            abe.maxRange = this.maxRange;

            abe.notifyUpdate();
        }
    }
}
