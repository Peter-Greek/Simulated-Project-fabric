package dev.simulated_team.simulated.backport.physics.api.physics.constraint;

import org.joml.Quaterniond;
import org.joml.Vector3dc;


/** Two anchor points and a relative orientation, per-axis motors added after. */
public record FreeConstraintConfiguration(Vector3dc anchorA, Vector3dc anchorB, Quaterniond orientation) {
}
