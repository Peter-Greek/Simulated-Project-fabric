package dev.simulated_team.simulated.backport.physics.api;

import dev.simulated_team.simulated.backport.physics.companion.math.BoundingBox3dc;
import dev.simulated_team.simulated.backport.physics.sublevel.ClientSubLevel;
import dev.simulated_team.simulated.backport.physics.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.backport.physics.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * The one question the rest of Simulated asks the physics engine: is this thing
 * inside a sub-level, and if so, where does it really sit?
 *
 * <p>On this stack the answer is always "not in a sub-level", so every lookup
 * returns {@code null}, every projection returns the position it was given, and
 * every velocity is zero. Ported files take their world-space branch, which is
 * correct for a world-placed block and is what V1 ships. See the package
 * documentation for how V2 removes this.
 */
public class SubLevelHelper {

    @Nullable
    public SubLevel getContaining(final Level level, final BlockPos pos) {
        return null;
    }

    @Nullable
    public SubLevel getContaining(final Level level, final Vec3 position) {
        return null;
    }

    @Nullable
    public SubLevel getContaining(final Level level, final Vector3dc position) {
        return null;
    }

    @Nullable
    public SubLevel getContaining(final Level level, final ChunkPos chunk) {
        return null;
    }

    @Nullable
    public SubLevel getContaining(final BlockEntity blockEntity) {
        return null;
    }

    @Nullable
    public SubLevel getContaining(final Entity entity) {
        return null;
    }

    @Nullable
    public ClientSubLevel getContainingClient(final BlockPos pos) {
        return null;
    }

    @Nullable
    public ClientSubLevel getContainingClient(final Vec3 position) {
        return null;
    }

    @Nullable
    public ClientSubLevel getContainingClient(final Vector3dc position) {
        return null;
    }

    @Nullable
    public ClientSubLevel getContainingClient(final BlockEntity blockEntity) {
        return null;
    }

    @Nullable
    public ClientSubLevel getContainingClient(final Entity entity) {
        return null;
    }

    /** The sub-level a player's camera is riding, if any. */
    @Nullable
    public SubLevel getTrackingSubLevel(final Entity entity) {
        return null;
    }

    @Nullable
    public SubLevel getTrackingOrVehicleSubLevel(final Entity entity) {
        return null;
    }

    /** With no sub-levels, a local position is already a world position. */
    public Vec3 projectOutOfSubLevel(final Level level, final Vec3 position) {
        return position;
    }

    public Vector3d projectOutOfSubLevel(final Level level, final Vector3dc position) {
        return new Vector3d(position);
    }

    public Vector3d projectOutOfSubLevel(final Level level, final Vector3dc position,
                                         final Vector3d destination) {
        return destination.set(position);
    }

    public Vec3 projectOutOfSubLevel(final Level level, final BlockPos pos) {
        return Vec3.atCenterOf(pos);
    }

    /**
     * Upstream measures through sub-level transforms so two points on different
     * bodies compare correctly. In world space that is the plain distance.
     */
    public double distanceSquaredWithSubLevels(final Level level, final Vec3 from, final Vec3 to) {
        return from.distanceToSqr(to);
    }

    public double distanceSquaredWithSubLevels(final Level level, final Vector3dc from, final Vector3dc to) {
        return from.distanceSquared(to);
    }

    public double distanceSquaredWithSubLevels(final Level level, final Vec3 from,
                                               final double x, final double y, final double z) {
        return from.distanceToSqr(x, y, z);
    }

    public double distanceSquaredWithSubLevels(final Level level,
                                               final double fromX, final double fromY, final double fromZ,
                                               final double toX, final double toY, final double toZ) {
        final double dx = toX - fromX;
        final double dy = toY - fromY;
        final double dz = toZ - fromZ;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Nothing is moving under its own physics, so every point is stationary. */
    public Vector3d getVelocity(final Level level, final Vector3dc position, final Vector3d destination) {
        return destination.set(0.0, 0.0, 0.0);
    }

    public Vector3d getVelocity(final Level level, final Vec3 position, final Vector3d destination) {
        return destination.set(0.0, 0.0, 0.0);
    }

    public Vector3d getVelocity(final Level level, @Nullable final SubLevel subLevel,
                                final Vector3dc position, final Vector3d destination) {
        return destination.set(0.0, 0.0, 0.0);
    }

    public Vector3d getVelocity(final Level level, @Nullable final SubLevel subLevel, final Vector3dc position) {
        return new Vector3d();
    }

    public List<SubLevel> getAllIntersecting(final Level level, final BoundingBox3dc bounds) {
        return Collections.emptyList();
    }

    /** No sub-level under the player, so the vanilla interpolated eye position stands. */
    public Vec3 getEyePositionInterpolated(final Entity entity, final float partialTicks) {
        return entity.getEyePosition(partialTicks);
    }
}
