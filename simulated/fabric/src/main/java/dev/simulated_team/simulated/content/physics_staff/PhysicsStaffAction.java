package dev.simulated_team.simulated.content.physics_staff;

import io.netty.buffer.ByteBuf;
import dev.simulated_team.simulated.backport.catnip.CatnipStreamCodecBuilders;
import dev.simulated_team.simulated.backport.net.StreamCodec;

/**
 * An action the player can complete using the physics staff
 */
public enum PhysicsStaffAction {
    STOP_DRAG,
    LOCK,
    START_DRAG;

    public static final StreamCodec<ByteBuf, PhysicsStaffAction> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(PhysicsStaffAction.class);
}
