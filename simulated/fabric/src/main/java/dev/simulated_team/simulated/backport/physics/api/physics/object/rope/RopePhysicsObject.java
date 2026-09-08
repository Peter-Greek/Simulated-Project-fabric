package dev.simulated_team.simulated.backport.physics.api.physics.object.rope;

import dev.simulated_team.simulated.backport.physics.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.simulated_team.simulated.backport.physics.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.ChunkPos;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * A simulated rope: an ordered chain of points with a fixed segment length.
 *
 * <p>The <em>shape</em> is real here — points are stored, added, removed, and
 * serialised exactly as upstream expects, so a rope saves, loads, and syncs. The
 * <em>simulation</em> is not: nothing integrates the points, so a rope hangs
 * wherever it was placed rather than swinging, and {@link #isActive()} is always
 * false so upstream's per-tick work is skipped. The solver lands in V3 with the
 * rest of the rope feature; see the package documentation.
 */
public class RopePhysicsObject {

    protected final ObjectArrayList<Vector3d> points = new ObjectArrayList<>();

    private final UUID uuid = UUID.randomUUID();
    private final double segmentLength;
    private double firstSegmentLength;

    public RopePhysicsObject(final Collection<Vector3d> points, final double segmentLength) {
        this.segmentLength = segmentLength;
        this.firstSegmentLength = segmentLength;
        for (final Vector3d point : points) {
            this.points.add(new Vector3d(point));
        }
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public List<Vector3d> getPoints() {
        return this.points;
    }

    public double getSegmentLength() {
        return this.segmentLength;
    }

    public void addPoint(final Vector3dc position) {
        this.points.add(new Vector3d(position));
    }

    public void removeFirstPoint() {
        if (!this.points.isEmpty()) {
            this.points.remove(0);
        }
    }

    public void setFirstSegmentLength(final double length) {
        this.firstSegmentLength = length;
    }

    public double getFirstSegmentLength() {
        return this.firstSegmentLength;
    }

    /** Whether the solver is stepping this rope. Nothing steps it here. */
    public boolean isActive() {
        return false;
    }

    /** Re-reads the rope's shape after something moved an attachment. */
    public void updatePose() {
    }

    public void onAddition(final SubLevelPhysicsSystem physicsSystem) {
    }

    public void onRemoved() {
    }

    public void onUnloaded(final SubLevelHoldingChunkMap holdingChunkMap, final ChunkPos chunkPos) {
    }

    /** The tick a client interpolates this rope's points against. */
    public int getInterpolationTick() {
        return 0;
    }

    /**
     * Whether a chunk is loaded enough for a rope to simulate in. Static here
     * because upstream calls it on the class.
     */
    public static boolean isChunkLoadedEnough(final net.minecraft.server.level.ServerLevel level,
                                              final int chunkX, final int chunkZ) {
        return false;
    }

    /** Ties one end of the rope to a body. Nothing is tied while there is no solver. */
    public void setAttachment(final RopeHandle.AttachmentPoint point, final Vector3dc localAnchor,
                              @javax.annotation.Nullable final Object subLevel) {
    }

    /** Where along a rope something is attached. Upstream also names it here. */
    public enum AttachmentPoint {
        START,
        END
    }
}
