package dev.simulated_team.simulated.backport.physics.api.physics.constraint;

/**
 * The six degrees of freedom a constraint can drive. Upstream iterates
 * {@link #LINEAR} and {@link #ANGULAR} to set a motor per axis.
 */
public enum ConstraintJointAxis {
    LINEAR_X,
    LINEAR_Y,
    LINEAR_Z,
    ANGULAR_X,
    ANGULAR_Y,
    ANGULAR_Z;

    public static final ConstraintJointAxis[] LINEAR = {LINEAR_X, LINEAR_Y, LINEAR_Z};

    public static final ConstraintJointAxis[] ANGULAR = {ANGULAR_X, ANGULAR_Y, ANGULAR_Z};
}
