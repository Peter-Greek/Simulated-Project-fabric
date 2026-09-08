package dev.simulated_team.simulated.network.packets.physics_staff;

import dev.simulated_team.simulated.backport.physics.companion.math.JOMLConversion;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffAction;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.util.SimCodecUtil;
import dev.simulated_team.simulated.backport.net.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.UUID;
import dev.simulated_team.simulated.backport.net.SimCodecs;

public class PhysicsStaffActionPacket implements CustomPacketPayload {

    public static CustomPacketPayload.Type<PhysicsStaffActionPacket> TYPE = new CustomPacketPayload.Type<>(Simulated.path("physics_staff_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PhysicsStaffActionPacket> CODEC = StreamCodec.composite(
            PhysicsStaffAction.STREAM_CODEC, packet -> packet.action,
            SimCodecs.UUID_CODEC, packet -> packet.subLevel,
            SimCodecUtil.STREAM_VECTOR3D, packet -> packet.location,
            PhysicsStaffActionPacket::new
    );

    protected final PhysicsStaffAction action;
    protected final UUID subLevel;
    protected final Vector3d location;

    public PhysicsStaffActionPacket(final PhysicsStaffAction action, final UUID subLevel, final Vector3dc location) {
        this.action = action;
        this.subLevel = subLevel;
        this.location = new Vector3d(location);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final PacketContext context) {
        final ServerLevel level = (ServerLevel) context.level();
        final Player player = context.player();

        if (!SimItems.PHYSICS_STAFF.isIn(player.getMainHandItem()) &&
                !SimItems.PHYSICS_STAFF.isIn(player.getOffhandItem())) {
            context.disconnect(Component.literal("Invalid packet"));
            return;
        }

        if (this.action == PhysicsStaffAction.LOCK) {
            PhysicsStaffServerHandler.get(level).toggleLock(this.subLevel);
        }

        if (this.action == PhysicsStaffAction.STOP_DRAG) {
            PhysicsStaffServerHandler.get(level).stopDragging(player.getUUID());
        }

        if (this.action == PhysicsStaffAction.LOCK) {
            final Vector3d beamStart = JOMLConversion.toJOML(player.getEyePosition());
            final Vector3d beamEnd = new Vector3d(this.location);

            final ChunkPos chunk = new ChunkPos(BlockPos.containing(this.location.x(), this.location.y(), this.location.z()));
            // 1.20.1 has no payload packet wrapper; the sink encodes the
            // payload itself.
            final PhysicsStaffBeamPacket beamPacket =
                    new PhysicsStaffBeamPacket(player.getUUID(), beamStart, beamEnd);

            for (final ServerPlayer otherPlayer : level.getChunkSource().chunkMap.getPlayers(chunk, false)) {
                if (otherPlayer == player) {
                    continue;
                }
                dev.simulated_team.simulated.backport.net.VeilPacketManager.player(otherPlayer).sendPacket(beamPacket);
            }
        }
    }
}
