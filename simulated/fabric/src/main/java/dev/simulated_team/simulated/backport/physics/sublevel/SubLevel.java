package dev.simulated_team.simulated.backport.physics.sublevel;

import dev.simulated_team.simulated.backport.physics.companion.math.BoundingBox3d;
import dev.simulated_team.simulated.backport.physics.companion.math.Pose3d;
import dev.simulated_team.simulated.backport.physics.sublevel.plot.LevelPlot;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * One rigid body and the blocks that ride on it.
 *
 * <p>Nothing constructs one on this stack — every lookup that would hand one out
 * returns {@code null} — so the type exists to keep upstream's field and
 * parameter declarations compiling. The members are the ones Simulated calls, so
 * a file that does get hold of one still reads as upstream wrote it.
 */
public class SubLevel {

    private final Level level;
    private final UUID uniqueId = UUID.randomUUID();
    private final Pose3d pose = new Pose3d();
    private String name = "";

    protected SubLevel(final Level level) {
        this.level = level;
    }

    public Level getLevel() {
        return this.level;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    /** The pose the server simulates. Identity while there is no physics. */
    public Pose3d logicalPose() {
        return this.pose;
    }

    /** The pose the client draws, interpolated between ticks. */
    public Pose3d renderPose() {
        return this.pose;
    }

    /** As {@link #renderPose()}; nothing moves, so the partial tick changes nothing. */
    public Pose3d renderPose(final float partialTicks) {
        return this.pose;
    }

    public Pose3d lastPose() {
        return this.pose;
    }

    public void updateLastPose() {
    }

    /** Re-reads the pose after something moved the body. Nothing moves here. */
    public void updatePose() {
    }

    public boolean isRemoved() {
        return true;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public LevelPlot getPlot() {
        return null;
    }

    public BoundingBox3d boundingBox() {
        return new BoundingBox3d();
    }
}
