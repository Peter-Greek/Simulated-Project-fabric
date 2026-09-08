package dev.simulated_team.simulated.backport.physics.api.physics.handle;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * A body in the solver.
 *
 * <p><b>Inert.</b> No handle is ever produced, so these exist for upstream's
 * declarations. Reads report a body at rest at the origin and writes are
 * discarded — see the package documentation.
 */
public class RigidBodyHandle {

    /** The body behind a sub-level. There is none, so there is no handle. */
    @javax.annotation.Nullable
    public static RigidBodyHandle of(
            final dev.simulated_team.simulated.backport.physics.sublevel.SubLevel subLevel) {
        return null;
    }

    public boolean isValid() {
        return false;
    }

    public void applyLinearAndAngularImpulse(final Vector3dc linear, final Vector3dc angular) {
    }

    public void applyLinearImpulse(final Vector3dc impulse) {
    }

    public void applyTorqueImpulse(final Vector3dc impulse) {
    }

    public Vector3d getLinearVelocity(final Vector3d destination) {
        return destination.set(0.0, 0.0, 0.0);
    }

    public Vector3d getLinearVelocity() {
        return new Vector3d();
    }

    public Vector3d getAngularVelocity(final Vector3d destination) {
        return destination.set(0.0, 0.0, 0.0);
    }

    public Vector3d getAngularVelocity() {
        return new Vector3d();
    }

    public Quaterniond getPreciseBodyRotation() {
        return new Quaterniond();
    }

    public Quaterniond getPreciseBodyRotation(final float partialTicks) {
        return new Quaterniond();
    }

    public Quaterniond getPreciseBodyRotation(final Quaterniond destination) {
        return destination.identity();
    }

    /**
     * The form that names the body being pushed, rather than pushing this one.
     * Static, because upstream calls it on the class.
     */
    public static void applyImpulseAtPoint(
            final dev.simulated_team.simulated.backport.physics.sublevel.SubLevel subLevel,
            final Vector3dc point, final Vector3dc impulse) {
    }

    public void applyImpulseAtPoint(final Vector3dc point, final Vector3dc impulse) {
    }

    public void applyForceAtPoint(final Vector3dc point, final Vector3dc force) {
    }

    public void applyForcesAndReset(
            final dev.simulated_team.simulated.backport.physics.api.physics.force.ForceTotal total) {
    }
}
