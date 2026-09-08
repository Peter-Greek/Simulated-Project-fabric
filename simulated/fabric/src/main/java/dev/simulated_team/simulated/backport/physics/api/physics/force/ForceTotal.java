package dev.simulated_team.simulated.backport.physics.api.physics.force;

import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * The force and torque accumulated for one body over a step.
 *
 * <p>Upstream builds one, hands it to the body, and resets. Nothing accumulates
 * here, so it is only ever the zero total.
 */
public class ForceTotal {

    private final Vector3d force = new Vector3d();
    private final Vector3d torque = new Vector3d();

    public ForceTotal() {
    }

    public ForceTotal(final Vector3dc force, final Vector3dc torque) {
        this.force.set(force);
        this.torque.set(torque);
    }

    public Vector3d force() {
        return this.force;
    }

    public Vector3d torque() {
        return this.torque;
    }

    public void addForce(final Vector3dc value) {
        this.force.add(value);
    }

    public void addTorque(final Vector3dc value) {
        this.torque.add(value);
    }

    public void applyLinearAndAngularImpulse(final Vector3dc linear, final Vector3dc angular) {
    }

    public void applyLinearImpulse(final Vector3dc impulse) {
    }

    public void applyTorqueImpulse(final Vector3dc impulse) {
    }

    public void applyImpulseAtPoint(
            final dev.simulated_team.simulated.backport.physics.sublevel.SubLevel subLevel,
            final Vector3dc point, final Vector3dc impulse) {
    }

    public void reset() {
        this.force.zero();
        this.torque.zero();
    }
}
