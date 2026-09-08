package dev.simulated_team.simulated.network.packets.name_plate;

import dev.simulated_team.simulated.network.PacketValidation;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlockEntity;
import dev.simulated_team.simulated.backport.net.ServerPacketContext;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import dev.simulated_team.simulated.backport.net.SimCodecs;

public record NameplateChangeNamePacket(BlockPos controllerPos, @Nullable String name) implements CustomPacketPayload {

    public static Type<NameplateChangeNamePacket> TYPE = new Type<>(Simulated.path("nameplate_change_name"));

    public static StreamCodec<RegistryFriendlyByteBuf, NameplateChangeNamePacket> CODEC = StreamCodec.composite(
            SimCodecs.BLOCK_POS, NameplateChangeNamePacket::controllerPos,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), (packet) -> Optional.ofNullable(packet.name()),
            NameplateChangeNamePacket::fromCodec);

    public static NameplateChangeNamePacket fromCodec(final BlockPos controllerPos, final Optional<String> name) {
        return new NameplateChangeNamePacket(controllerPos, name.orElse(null));
    }

    public void handle(final ServerPacketContext context) {
        if (this.name != null && (this.name.length() > 64 || this.name.chars().anyMatch(Character::isISOControl))) return;
        if (!PacketValidation.canInteract(context.player(), this.controllerPos)) return;
        final Level level = context.level();
        if (level.getBlockEntity(this.controllerPos()) instanceof final NameplateBlockEntity nbe && nbe.allowsEditing()) {
            nbe.setName(this.name, true, context.player());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
