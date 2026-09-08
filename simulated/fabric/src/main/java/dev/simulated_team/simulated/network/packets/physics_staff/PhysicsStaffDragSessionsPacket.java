package dev.simulated_team.simulated.network.packets.physics_staff;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.util.SimCodecUtil;
import dev.simulated_team.simulated.backport.net.PacketContext;
import dev.simulated_team.simulated.backport.catnip.CatnipStreamCodecBuilders;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

import java.util.List;
import java.util.UUID;
import dev.simulated_team.simulated.backport.net.SimCodecs;

public record PhysicsStaffDragSessionsPacket(ResourceKey<Level> dimension, List<Pair<UUID, Vector3d>> sessions) implements CustomPacketPayload {
    public static Type<PhysicsStaffDragSessionsPacket> TYPE = new Type<>(Simulated.path("physics_staff_drag_sessions"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, PhysicsStaffDragSessionsPacket> CODEC = StreamCodec.composite(
            SimCodecs.dimension(), i -> i.dimension,
            CatnipStreamCodecBuilders.list(SimCodecs.catnipPair(SimCodecs.UUID_CODEC, SimCodecUtil.STREAM_VECTOR3D)), i -> i.sessions,
            PhysicsStaffDragSessionsPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final PacketContext context) {
        SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.setServerDragSessions(this.dimension, this.sessions);
    }
}
