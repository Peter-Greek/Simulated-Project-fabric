package dev.simulated_team.simulated.backport.physics.api.physics.force;

import org.joml.Vector3dc;
import java.util.Collections;
import java.util.List;

/**
 * Stand-in for Sable's {@code QueuedForceGroup}. Nothing on this stack constructs
 * one — see the package documentation — so this exists to keep upstream's
 * declarations compiling until V2 brings the engine in.
 */
public class QueuedForceGroup {

    /** A point force: where it acts, and the force applied there. */
    public record PointForce(Vector3dc point, Vector3dc force) {
    }

    public void addForce(final Vector3dc point, final Vector3dc force) {
    }

    public void recordPointForce(final Vector3dc point, final Vector3dc force) {
    }

    public void applyAndRecordPointForce(final Vector3dc point, final Vector3dc force) {
    }

    public dev.simulated_team.simulated.backport.physics.api.physics.force.ForceTotal getForceTotal() {
        return new ForceTotal();
    }

    public List<PointForce> getForces() {
        return Collections.emptyList();
    }

    public List<PointForce> getRecordedPointForces() {
        return Collections.emptyList();
    }
}
