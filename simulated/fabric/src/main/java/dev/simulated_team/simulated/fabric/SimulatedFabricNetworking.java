package dev.simulated_team.simulated.fabric;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Small Fabric networking bridge used by the temporary Create transport.
 *
 * Create's ControlledContraptionEntity intentionally ignores vanilla client
 * lerp/teleport packets because Create normally drives these entities from a
 * synchronized controller. The Physics Assembler compatibility transport moves
 * the entity directly, so its position must be mirrored to tracking clients.
 */
public final class SimulatedFabricNetworking {
    public static final ResourceLocation CONTRAPTION_POSITION =
            new ResourceLocation(SimulatedFabric.MOD_ID, "contraption_position");

    private SimulatedFabricNetworking() {
    }

    public static void syncContraptionPosition(final ControlledContraptionEntity entity) {
        for (final ServerPlayer player : PlayerLookup.tracking(entity)) {
            final FriendlyByteBuf buffer = PacketByteBufs.create();
            buffer.writeInt(entity.getId());
            buffer.writeDouble(entity.getX());
            buffer.writeDouble(entity.getY());
            buffer.writeDouble(entity.getZ());
            ServerPlayNetworking.send(player, CONTRAPTION_POSITION, buffer);
        }
    }
}
