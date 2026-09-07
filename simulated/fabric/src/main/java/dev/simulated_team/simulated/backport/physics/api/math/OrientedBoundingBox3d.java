package dev.simulated_team.simulated.backport.physics.api.math;

import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Stand-in for Sable's {@code OrientedBoundingBox3d}. Nothing on this stack constructs
 * one — see the package documentation — so this exists to keep upstream's
 * declarations compiling until V2 brings the engine in.
 */
public class OrientedBoundingBox3d {

    /** The world axes, as Sable names them. Plain constants, not engine state. */
    public static final Vector3dc RIGHT = new Vector3d(1.0, 0.0, 0.0);

    public static final Vector3dc UP = new Vector3d(0.0, 1.0, 0.0);

    public static final Vector3dc FORWARD = new Vector3d(0.0, 0.0, 1.0);
}
