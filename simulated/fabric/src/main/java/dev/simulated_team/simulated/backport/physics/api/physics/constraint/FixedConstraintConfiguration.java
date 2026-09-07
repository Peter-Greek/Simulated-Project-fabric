package dev.simulated_team.simulated.backport.physics.api.physics.constraint;

import org.joml.Quaterniond;
import org.joml.Vector3dc;


/** Two anchor points welded at a fixed relative orientation. */
public record FixedConstraintConfiguration(Vector3dc anchorA, Vector3dc anchorB, Quaterniond orientation) {
}
