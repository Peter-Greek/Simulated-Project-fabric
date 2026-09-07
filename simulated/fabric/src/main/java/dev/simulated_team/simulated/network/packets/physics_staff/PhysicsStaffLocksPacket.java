package dev.simulated_team.simulated.network.packets.physics_staff;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.backport.net.PacketContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import dev.simulated_team.simulated.backport.catnip.CatnipStreamCodecBuilders;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import dev.simulated_team.simulated.backport.net.SimCodecs;

public class PhysicsStaffLocksPacket implements CustomPacketPayload {

    public static Type<PhysicsStaffLocksPacket> TYPE = new Type<>(Simulated.path("physics_staff_locks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PhysicsStaffLocksPacket> CODEC = StreamCodec.composite(
            SimCodecs.dimension(), packet -> packet.dimension,
            CatnipStreamCodecBuilders.list(SimCodecs.UUID_CODEC), packet -> packet.locks,
            PhysicsStaffLocksPacket::new
    );

    protected final List<UUID> locks;
    private final ResourceKey<Level> dimension;

    public PhysicsStaffLocksPacket(final ResourceKey<Level> dimension, final Collection<UUID> locks) {
        this.dimension = dimension;
        this.locks = new ObjectArrayList<>(locks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final PacketContext context) {
        SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.setLocks(this.dimension, this.locks);
    }
}
