package dev.simulated_team.simulated.fabric;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblyContraption;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative temporary vehicle controls for the Create-backed assembler transport. */
public final class SimulatedFabricNetworking {
    public static final ResourceLocation CONTRAPTION_POSITION =
            new ResourceLocation(SimulatedFabric.MOD_ID, "contraption_position");
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
    private static final int TRANSLATION_MASK = FLIGHT_FORWARD | FLIGHT_BACK | FLIGHT_UP | FLIGHT_DOWN;

    private static final double MAX_FLIGHT_SPEED = 0.35D;
    private static final double ACCELERATION = 0.22D;
    private static final double BRAKING = 0.38D;
    private static final double COLLISION_STEP = 0.125D;
    private static final double STOP_EPSILON_SQR = 0.000025D;
    private static final double MAX_YAW_SPEED = 3.0D;
    private static final double YAW_ACCELERATION = 0.30D;
    private static final double YAW_BRAKING = 0.45D;
    private static final double ROTATION_COLLISION_STEP = 1.0D;

    private static final Map<UUID, FlightSession> FLIGHT_SESSIONS = new HashMap<>();
    private static boolean receiversRegistered;

    private SimulatedFabricNetworking() {
    }

    public static void registerServerReceivers() {
        if (receiversRegistered) {
            return;
        }
        receiversRegistered = true;

        // The packet only records intent. Movement is applied on the server tick
        // below, so a craft keeps accelerating and braking at a fixed rate no
        // matter how often the client's input actually changes.
        ServerPlayNetworking.registerGlobalReceiver(
                FLIGHT_INPUT,
                (server, player, handler, buffer, responseSender) -> {
                    final int inputMask = buffer.readUnsignedByte() & VALID_FLIGHT_MASK;
                    server.execute(() -> {
                        final FlightSession session = FLIGHT_SESSIONS.get(player.getUUID());
                        if (session != null) {
                            session.inputMask = inputMask;
                        }
                    });
                });
    }

    public static void clearFlightControls() {
        FLIGHT_SESSIONS.clear();
    }

    /** Advances every live helm exactly once per server tick. */
    public static void tickFlightSessions(final MinecraftServer server) {
        if (FLIGHT_SESSIONS.isEmpty()) {
            return;
        }

        for (final UUID playerId : List.copyOf(FLIGHT_SESSIONS.keySet())) {
            final ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                FLIGHT_SESSIONS.remove(playerId);
                continue;
            }

            final FlightSession session = FLIGHT_SESSIONS.get(playerId);
            if (session != null) {
                tickFlightSession(player, session);
            }
        }
    }

    /**
     * Releases a helm when its pilot disconnects. Without this the session
     * outlives the player and the craft stays flagged as piloted.
     */
    public static void handlePlayerDisconnect(final ServerPlayer player) {
        final FlightSession session = FLIGHT_SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }

        final Entity resolved = player.serverLevel().getEntity(session.contraptionId);
        if (resolved instanceof final ControlledContraptionEntity controlled && controlled.isAlive()) {
            controlled.setContraptionMotion(Vec3.ZERO);
            if (controlled.getControllingPlayer().map(player.getUUID()::equals).orElse(false)) {
                controlled.setControllingPlayer(null);
            }
            syncContraptionPosition(controlled);
        }
    }

    /** Fallback helm entry point used by the moving Physics Assembler itself. */
    public static void beginFlightControl(final ServerPlayer player,
                                          final ControlledContraptionEntity target,
                                          final Direction forward) {
        beginFlightControl(player, target, PhysicsAssemblyContraption.HELM_SEAT, forward);
    }

    /**
     * Starts vehicle control from a specific logical rider point on the moving
     * craft.
     *
     * <p>{@code forward} is the direction the control block faces in the
     * contraption's own local space — the way the craft drives when the pilot
     * holds W. It is local, not world: the contraption's current rotation is
     * applied to it every tick, so the craft keeps steering relative to itself
     * after it has turned.
     */
    public static void beginFlightControl(final ServerPlayer player,
                                          final ControlledContraptionEntity target,
                                          final BlockPos localSeat,
                                          final Direction forward) {
        if (!target.isAlive() || !(target.getContraption() instanceof final PhysicsAssemblyContraption physicsAssembly)) {
            sendFlightControlState(player, false, "Physics Assembler helm is unavailable");
            return;
        }

        final UUID playerId = player.getUUID();
        final UUID existingController = target.getControllingPlayer().orElse(null);
        if (existingController != null && !existingController.equals(playerId)) {
            final Entity controller = player.serverLevel().getPlayerByUUID(existingController);
            if (controller != null) {
                sendFlightControlState(player, false, "This Physics Assembler is already being piloted");
                return;
            }
        }

        final FlightSession previous = FLIGHT_SESSIONS.get(playerId);
        if (previous != null && !previous.contraptionId.equals(target.getUUID())) {
            stopFlightControl(player, "Switched Physics Assembler helm");
        }

        target.setRotationAxis(Direction.Axis.Y);
        target.setControllingPlayer(playerId);
        target.setContraptionMotion(Vec3.ZERO);

        final int helmSeat = physicsAssembly.ensureHelmSeat(localSeat);
        if (player.getVehicle() != target) {
            player.stopRiding();
            target.addSittingPassenger(player, helmSeat);
        }

        FLIGHT_SESSIONS.put(playerId, new FlightSession(target.getUUID(), forward));
        syncContraptionPosition(target);
        sendFlightControlState(player, true,
                "Vehicle helm engaged: W/S thrust, A/D yaw, Space ascend, Ctrl descend, Shift dismount");
    }

    /**
     * Parks the craft on a cardinal heading before Create places its blocks back
     * into the world. Returns false when the snapped orientation would collide.
     */
    public static boolean prepareForDisassembly(final ControlledContraptionEntity active) {
        if (!active.isAlive() || !(active.getContraption() instanceof PhysicsAssemblyContraption)) {
            return false;
        }

        releaseCraftControllers(active, "Physics Assembler helm released for disassembly");
        active.setContraptionMotion(Vec3.ZERO);

        final float previousAngle = active.getAngle(1.0F);
        final float snappedAngle = Math.round(previousAngle / 90.0F) * 90.0F;
        active.setRotationAxis(Direction.Axis.Y);
        active.setAngle(snappedAngle);
        if (ContraptionCollider.collideBlocks(active)) {
            active.setAngle(previousAngle);
            syncContraptionPosition(active);
            return false;
        }

        syncContraptionPosition(active);
        return true;
    }

    public static void syncContraptionPosition(final ControlledContraptionEntity entity) {
        for (final ServerPlayer player : PlayerLookup.tracking(entity)) {
            final FriendlyByteBuf buffer = PacketByteBufs.create();
            buffer.writeInt(entity.getId());
            buffer.writeDouble(entity.getX());
            buffer.writeDouble(entity.getY());
            buffer.writeDouble(entity.getZ());
            buffer.writeFloat(entity.getAngle(1.0F));
            ServerPlayNetworking.send(player, CONTRAPTION_POSITION, buffer);
        }
    }

    private static void tickFlightSession(final ServerPlayer player, final FlightSession session) {
        final Entity resolved = player.serverLevel().getEntity(session.contraptionId);
        if (!(resolved instanceof final ControlledContraptionEntity active)
                || !active.isAlive()
                || !(active.getContraption() instanceof PhysicsAssemblyContraption)) {
            stopFlightControl(player, "Helm released: assembly is no longer active");
            return;
        }

        if (player.getVehicle() != active
                || !active.getControllingPlayer().map(player.getUUID()::equals).orElse(false)) {
            stopFlightControl(player, "Physics Assembler helm released");
            return;
        }

        final int inputMask = session.inputMask;
        final Vec3 positionBefore = active.position();
        final float angleBefore = active.getAngle(1.0F);

        final int turn = ((inputMask & FLIGHT_LEFT) != 0 ? -1 : 0)
                + ((inputMask & FLIGHT_RIGHT) != 0 ? 1 : 0);
        final double targetYawVelocity = turn * MAX_YAW_SPEED;
        final double yawResponse = turn == 0 ? YAW_BRAKING : YAW_ACCELERATION;
        session.yawVelocity += (targetYawVelocity - session.yawVelocity) * yawResponse;
        if (Math.abs(session.yawVelocity) < 0.02D) {
            session.yawVelocity = 0.0D;
        }

        // Negated: Create rotates a contraption anticlockwise for a rising
        // angle (VecHelper.rotate takes local +Z toward +X), so turning right
        // means decreasing the angle.
        if (session.yawVelocity != 0.0D && !rotateWithCollision(active, -session.yawVelocity)) {
            session.yawVelocity = 0.0D;
            notifyCollision(player, session, "Physics Assembler yaw blocked by world collision");
        }

        final int translationInput = inputMask & TRANSLATION_MASK;
        final Vec3 desiredDirection = desiredDirection(translationInput, active, session.localForward);
        final Vec3 desiredVelocity = desiredDirection.scale(MAX_FLIGHT_SPEED);
        final double response = translationInput == 0 ? BRAKING : ACCELERATION;
        session.velocity = session.velocity.add(desiredVelocity.subtract(session.velocity).scale(response));
        if (session.velocity.lengthSqr() < STOP_EPSILON_SQR) {
            session.velocity = Vec3.ZERO;
        }

        if (session.velocity != Vec3.ZERO && !moveWithCollision(active, session.velocity)) {
            session.velocity = Vec3.ZERO;
            notifyCollision(player, session, "Physics Assembler movement blocked by world collision");
        } else if (session.velocity != Vec3.ZERO || session.yawVelocity != 0.0D) {
            session.collisionNotified = false;
        }

        active.setContraptionMotion(Vec3.ZERO);

        // Only tell trackers about the craft when it actually moved. A parked
        // helm otherwise broadcasts an identical position to every nearby player
        // twenty times a second.
        if (!active.position().equals(positionBefore) || active.getAngle(1.0F) != angleBefore) {
            syncContraptionPosition(active);
        }
    }

    /**
     * The craft drives the way its control block points, not the way world north
     * happens to lie. The local forward vector is put through the contraption's
     * own {@code applyRotation}, so the heading tracks the craft as it yaws and
     * matches exactly what Create renders and collides.
     */
    private static Vec3 desiredDirection(final int inputMask,
                                         final ControlledContraptionEntity active,
                                         final Direction localForward) {
        final Vec3 rotated = active.applyRotation(Vec3.atLowerCornerOf(localForward.getNormal()), 1.0F);
        final Vec3 flattened = new Vec3(rotated.x, 0.0D, rotated.z);
        final Vec3 forward = flattened.lengthSqr() < 1.0E-6D ? Vec3.ZERO : flattened.normalize();
        Vec3 desired = Vec3.ZERO;

        if ((inputMask & FLIGHT_FORWARD) != 0) {
            desired = desired.add(forward);
        }
        if ((inputMask & FLIGHT_BACK) != 0) {
            desired = desired.subtract(forward);
        }
        if ((inputMask & FLIGHT_UP) != 0) {
            desired = desired.add(0.0D, 1.0D, 0.0D);
        }
        if ((inputMask & FLIGHT_DOWN) != 0) {
            desired = desired.add(0.0D, -1.0D, 0.0D);
        }

        return desired.lengthSqr() > 1.0D ? desired.normalize() : desired;
    }

    private static boolean rotateWithCollision(final ControlledContraptionEntity active,
                                               final double yawDelta) {
        final int steps = Math.max(1, (int) Math.ceil(Math.abs(yawDelta) / ROTATION_COLLISION_STEP));
        final float step = (float) (yawDelta / steps);
        for (int i = 0; i < steps; i++) {
            final float previous = active.getAngle(1.0F);
            active.setAngle(previous + step);
            if (ContraptionCollider.collideBlocks(active)) {
                active.setAngle(previous);
                return false;
            }
        }
        return true;
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
                return false;
            }
        }
        return true;
    }

    private static void notifyCollision(final ServerPlayer player,
                                        final FlightSession session,
                                        final String message) {
        if (!session.collisionNotified) {
            player.displayClientMessage(Component.literal(message), true);
            session.collisionNotified = true;
        }
    }

    private static void releaseCraftControllers(final ControlledContraptionEntity active,
                                                final String message) {
        final List<UUID> players = new ArrayList<>();
        for (final Map.Entry<UUID, FlightSession> entry : FLIGHT_SESSIONS.entrySet()) {
            if (entry.getValue().contraptionId.equals(active.getUUID())) {
                players.add(entry.getKey());
            }
        }

        if (active.level() instanceof final ServerLevel serverLevel) {
            for (final UUID playerId : players) {
                final ServerPlayer player = (ServerPlayer) serverLevel.getPlayerByUUID(playerId);
                if (player != null) {
                    stopFlightControl(player, message);
                } else {
                    FLIGHT_SESSIONS.remove(playerId);
                }
            }
        } else {
            players.forEach(FLIGHT_SESSIONS::remove);
        }

        for (final Entity passenger : List.copyOf(active.getPassengers())) {
            passenger.stopRiding();
        }
        active.setControllingPlayer(null);
    }

    private static void stopFlightControl(final ServerPlayer player, final String message) {
        final FlightSession session = FLIGHT_SESSIONS.remove(player.getUUID());
        if (session != null) {
            final Entity resolved = player.serverLevel().getEntity(session.contraptionId);
            if (resolved instanceof final ControlledContraptionEntity controlled && controlled.isAlive()) {
                controlled.setContraptionMotion(Vec3.ZERO);
                if (controlled.getControllingPlayer().map(player.getUUID()::equals).orElse(false)) {
                    controlled.setControllingPlayer(null);
                }
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
        /** Direction the control block faces in contraption-local space. */
        private final Direction localForward;
        private Vec3 velocity = Vec3.ZERO;
        private double yawVelocity;
        /** Latest intent from the pilot's client; applied on every server tick. */
        private int inputMask;
        private boolean collisionNotified;

        private FlightSession(final UUID contraptionId, final Direction localForward) {
            this.contraptionId = contraptionId;
            this.localForward = localForward;
        }
    }
}
