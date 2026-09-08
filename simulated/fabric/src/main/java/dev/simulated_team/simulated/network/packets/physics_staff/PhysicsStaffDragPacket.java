package dev.simulated_team.simulated.network.packets.physics_staff;

import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.backport.physics.util.SableBufferUtils;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import dev.simulated_team.simulated.backport.net.ServerPacketContext;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.UUID;

public record PhysicsStaffDragPacket(UUID subLevel, Vector3dc playerRelativeGoal,
                                     Vector3dc localAnchor, Quaterniondc orientation) implements CustomPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, PhysicsStaffDragPacket> CODEC = StreamCodec.of((buf, value) -> value.write(buf), PhysicsStaffDragPacket::read);
    public static Type<PhysicsStaffDragPacket> TYPE = new Type<>(Simulated.path("physics_staff_drag"));

    private static PhysicsStaffDragPacket read(final RegistryFriendlyByteBuf buf) {
        return new PhysicsStaffDragPacket(buf.readUUID(), SableBufferUtils.read(buf, new Vector3d()), SableBufferUtils.read(buf, new Vector3d()), SableBufferUtils.read(buf, new Quaterniond()));
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.subLevel);
        SableBufferUtils.write(buf, this.playerRelativeGoal);
        SableBufferUtils.write(buf, this.localAnchor);
        SableBufferUtils.write(buf, this.orientation);
    }

    public void handle(final ServerPacketContext context) {
        final ServerPlayer player = context.player();
        final Level level = player.level();

        // PhysicsStaffActionPacket already refuses a client that is not holding the
        // staff; the drag packet has to agree, or the check is bypassable.
        if (!SimItems.PHYSICS_STAFF.isIn(player.getMainHandItem())
                && !SimItems.PHYSICS_STAFF.isIn(player.getOffhandItem())) {
            return;
        }

        PhysicsStaffServerHandler.get((ServerLevel) level)
                .drag(player.getGameProfile().getId(), this.subLevel, this.playerRelativeGoal, this.localAnchor, this.orientation);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
