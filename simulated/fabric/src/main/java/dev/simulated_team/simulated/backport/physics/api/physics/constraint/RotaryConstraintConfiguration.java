package dev.simulated_team.simulated.backport.physics.api.physics.constraint;

import org.joml.Vector3dc;


/** A hinge: two anchors and the axis they turn about. */
public record RotaryConstraintConfiguration(Vector3dc anchorA, Vector3dc anchorB,
                                            Vector3dc axisA, Vector3dc axisB) {

    public RotaryConstraintConfiguration(final Vector3dc anchorA, final Vector3dc anchorB, final Vector3dc axis) {
        this(anchorA, anchorB, axis, axis);
    }
}
