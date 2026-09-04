package dev.simulated_team.simulated.fabric;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblyContraption;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fabric networking bridge for the temporary Create transport.
 *
 * Position synchronization remains explicit because ControlledContraptionEntity
 * does not consume vanilla teleport interpolation like a normal entity. Flight
 * input is client-requested but all craft selection, rate limiting, movement,
 * collision checks and speed limits are authoritative on the server.
 */
public final class SimulatedFabricNetworking {
    public static final ResourceLocation CONTRAPTION_POSITION =
            new ResourceLocation(SimulatedFabric.MOD_ID, "contraption_position");
    public static final ResourceLocation FLIGHT_CONTROL_TOGGLE =
            new ResourceLocation(SimulatedFabric.MOD_ID, "flight_control_toggle");
    public static final ResourceLocation FLIGHT_CONTROL_STATE =
            new ResourceLocation(SimulatedFabric.MOD_ID, "flight_control_state");
    public static final ResourceLocation FLIGHT_INPUT =
            new ResourceLocation(SimulatedFabric.MOD_ID, "flight_input");

    public static final int FLIGHT_FORWARD = 1;
    public static final int FLIGHT_BACK = 1 << 1;
    public static final int FLIGHT_LEFT = 1 << 2;
    public static final int FLIGHT_RIGHT = 1 << 3;
    public static final int FLIGHT_UP = 1 << 4;
    public static final int FLIGHT_DOWN = 1 << 5;
    private static final int VALID_FLIGHT_MASK = FLIGHT_FORWARD | FLIGHT_BACK | FLIGHT_LEFT
            | FLIGHT_RIGHT | FLIGHT_UP | FLIGHT_DOWN;

    private static final double ENGAGE_DISTANCE_SQR = 32.0D * 32.0D;
    private static final double CONTROL_DISTANCE_SQR = 96.0D * 96.0D;
    private static final double MAX_FLIGHT_SPEED = 0.35D;
    private static final double ACCELERATION = 0.22D;
    private static final double BRAKING = 0.38D;
    private static final double COLLISION_STEP = 0.125D;
    private static final double STOP_EPSILON_SQR = 0.000025D;

    private static final Map<UUID, FlightSession> FLIGHT_SESSIONS = new HashMap<>();
    private static boolean receiversRegistered;

    private SimulatedFabricNetworking() {
    }

    public static void registerServerReceivers() {
        if (receiversRegistered) {
            return;
        }
        receiversRegistered = true;

        ServerPlayNetworking.registerGlobalReceiver(
                FLIGHT_CONTROL_TOGGLE,
                (server, player, handler, buffer, responseSender) ->
                        server.execute(() -> toggleFlightControl(player)));

        ServerPlayNetworking.registerGlobalReceiver(
                FLIGHT_INPUT,
                (server, player, handler, buffer, responseSender) -> {
                    final int inputMask = buffer.readUnsignedByte() & VALID_FLIGHT_MASK;
                    final float yaw = buffer.readFloat();
                    server.execute(() -> applyFlightInput(player, inputMask, yaw));
                });
    }

    public static void clearFlightControls() {
        FLIGHT_SESSIONS.clear();
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

    private static void toggleFlightControl(final ServerPlayer player) {
        final UUID playerId = player.getUUID();
        if (FLIGHT_SESSIONS.containsKey(playerId)) {
            stopFlightControl(player, "Physics Assembler flight controls disengaged");
            return;
        }

        final ControlledContraptionEntity target = findNearestPhysicsAssembly(player);
        if (target == null) {
            sendFlightControlState(player, false,
                    "No assembled Physics Assembler within 32 blocks");
            return;
        }

        FLIGHT_SESSIONS.put(playerId, new FlightSession(target.getUUID()));
        target.setContraptionMotion(Vec3.ZERO);
        sendFlightControlState(player, true,
                "Flight controls engaged: arrows move, Page Up/Down changes altitude, G disengages");
    }

    private static ControlledContraptionEntity findNearestPhysicsAssembly(final ServerPlayer player) {
        ControlledContraptionEntity nearest = null;
        double nearestDistance = ENGAGE_DISTANCE_SQR;

        for (final Entity entity : player.serverLevel().getAllEntities()) {
            if (!(entity instanceof final ControlledContraptionEntity controlled)
                    || !controlled.isAlive()
                    || !(controlled.getContraption() instanceof PhysicsAssemblyContraption)) {
                continue;
            }

            final double distance = player.distanceToSqr(controlled);
            if (distance <= nearestDistance) {
                nearest = controlled;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static void applyFlightInput(final ServerPlayer player, final int inputMask, final float yawDegrees) {
        final FlightSession session = FLIGHT_SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }

        final Entity resolved = player.serverLevel().getEntity(session.contraptionId);
        if (!(resolved instanceof final ControlledContraptionEntity active)
                || !active.isAlive()
                || !(active.getContraption() instanceof PhysicsAssemblyContraption)) {
            stopFlightControl(player, "Flight controls disengaged: assembly is no longer active");
            return;
        }

        if (player.distanceToSqr(active) > CONTROL_DISTANCE_SQR) {
            stopFlightControl(player, "Flight controls disengaged: assembly is too far away");
            return;
        }

        final long gameTime = player.serverLevel().getGameTime();
        if (session.lastInputGameTime == gameTime) {
            return;
        }
        session.lastInputGameTime = gameTime;

        final Vec3 desiredDirection = desiredDirection(inputMask, yawDegrees);
        final Vec3 desiredVelocity = desiredDirection.scale(MAX_FLIGHT_SPEED);
        final double response = inputMask == 0 ? BRAKING : ACCELERATION;
        session.velocity = session.velocity.add(desiredVelocity.subtract(session.velocity).scale(response));
        if (session.velocity.lengthSqr() < STOP_EPSILON_SQR) {
            session.velocity = Vec3.ZERO;
        }

        if (session.velocity == Vec3.ZERO) {
            active.setContraptionMotion(Vec3.ZERO);
            return;
        }

        if (!moveWithCollision(active, session.velocity)) {
            session.velocity = Vec3.ZERO;
            if (!session.collisionNotified) {
                player.displayClientMessage(Component.literal("Physics Assembler flight blocked by world collision"), true);
                session.collisionNotified = true;
            }
        } else {
            session.collisionNotified = false;
        }
    }

    private static Vec3 desiredDirection(final int inputMask, final float yawDegrees) {
        final double yaw = Math.toRadians(yawDegrees);
        final Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        final Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 desired = Vec3.ZERO;

        if ((inputMask & FLIGHT_FORWARD) != 0) {
            desired = desired.add(forward);
        }
        if ((inputMask & FLIGHT_BACK) != 0) {
            desired = desired.subtract(forward);
        }
        if ((inputMask & FLIGHT_LEFT) != 0) {
            desired = desired.subtract(right);
        }
        if ((inputMask & FLIGHT_RIGHT) != 0) {
            desired = desired.add(right);
        }
        if ((inputMask & FLIGHT_UP) != 0) {
            desired = desired.add(0.0D, 1.0D, 0.0D);
        }
        if ((inputMask & FLIGHT_DOWN) != 0) {
            desired = desired.add(0.0D, -1.0D, 0.0D);
        }

        return desired.lengthSqr() > 1.0D ? desired.normalize() : desired;
    }

    private static boolean moveWithCollision(final ControlledContraptionEntity active, final Vec3 movement) {
        final double distance = movement.length();
        final int steps = Math.max(1, (int) Math.ceil(distance / COLLISION_STEP));
        final Vec3 step = movement.scale(1.0D / steps);

        for (int i = 0; i < steps; i++) {
            final Vec3 previous = active.position();
            active.setContraptionMotion(step);
            active.move(step.x, step.y, step.z);
            if (ContraptionCollider.collideBlocks(active)) {
                active.setPos(previous.x, previous.y, previous.z);
                active.setContraptionMotion(Vec3.ZERO);
                syncContraptionPosition(active);
                return false;
            }
        }

        active.setContraptionMotion(Vec3.ZERO);
        syncContraptionPosition(active);
        return true;
    }

    private static void stopFlightControl(final ServerPlayer player, final String message) {
        final FlightSession session = FLIGHT_SESSIONS.remove(player.getUUID());
        if (session != null) {
            final Entity resolved = player.serverLevel().getEntity(session.contraptionId);
            if (resolved instanceof final ControlledContraptionEntity controlled && controlled.isAlive()) {
                controlled.setContraptionMotion(Vec3.ZERO);
                syncContraptionPosition(controlled);
            }
        }
        sendFlightControlState(player, false, message);
    }

    private static void sendFlightControlState(final ServerPlayer player,
                                               final boolean engaged,
                                               final String message) {
        final FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeBoolean(engaged);
        buffer.writeUtf(message, 256);
        ServerPlayNetworking.send(player, FLIGHT_CONTROL_STATE, buffer);
    }

    private static final class FlightSession {
        private final UUID contraptionId;
        private Vec3 velocity = Vec3.ZERO;
        private long lastInputGameTime = Long.MIN_VALUE;
        private boolean collisionNotified;

        private FlightSession(final UUID contraptionId) {
            this.contraptionId = contraptionId;
        }
    }
}
