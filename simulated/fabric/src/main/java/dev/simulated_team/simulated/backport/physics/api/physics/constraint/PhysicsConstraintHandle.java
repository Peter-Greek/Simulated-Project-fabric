package dev.simulated_team.simulated.backport.physics.api.physics.constraint;

/**
 * A live constraint in the solver. Nothing on this stack creates one — the
 * pipeline hands back {@code null} — so the methods exist for the declarations
 * and never run.
 */
public class PhysicsConstraintHandle {

    public void setMotor(final ConstraintJointAxis axis, final double target, final double stiffness,
                         final double damping, final boolean enabled, final double maxForce) {
    }

    public void setTargetOrientation(final org.joml.Quaterniondc orientation) {
    }

    public void remove() {
    }

    public boolean isValid() {
        return false;
    }
}
