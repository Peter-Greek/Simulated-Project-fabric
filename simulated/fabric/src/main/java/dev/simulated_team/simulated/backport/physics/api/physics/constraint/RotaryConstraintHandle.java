package dev.simulated_team.simulated.backport.physics.api.physics.constraint;

/** A hinge. See {@link PhysicsConstraintHandle}. */
public class RotaryConstraintHandle extends PhysicsConstraintHandle {

    /** Whether the two bodies a hinge joins may still collide with each other. */
    public void setContactsEnabled(final boolean enabled) {
    }


    /** Upstream's stand-in for "no hinge configured". */
    public static final RotaryConstraintHandle DEFAULT = new RotaryConstraintHandle();

    /** The single axis a hinge drives: the one it turns about. */
    public static final ConstraintJointAxis DEFAULT_AXIS = ConstraintJointAxis.ANGULAR_Y;
}
