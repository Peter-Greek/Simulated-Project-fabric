package dev.simulated_team.simulated.backport.physics.companion.math;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/** Mutable rigid transform. See {@link Pose3dc}. */
public class Pose3d implements Pose3dc {

    private final Vector3d position;
    private final Quaterniond orientation;
    private final Vector3d rotationPoint;
    private final Vector3d scale;

    public Pose3d() {
        this(new Vector3d(), new Quaterniond(), new Vector3d(), new Vector3d(1.0));
    }

    public Pose3d(final Vector3dc position, final Quaterniond orientation,
                  final Vector3dc rotationPoint, final Vector3dc scale) {
        this.position = new Vector3d(position);
        this.orientation = new Quaterniond(orientation);
        this.rotationPoint = new Vector3d(rotationPoint);
        this.scale = new Vector3d(scale);
    }

    public Pose3d(final Pose3dc other) {
        this(other.position(), other.orientation(), other.rotationPoint(), other.scale());
    }

    @Override
    public Vector3d position() {
        return this.position;
    }

    @Override
    public Quaterniond orientation() {
        return this.orientation;
    }

    @Override
    public Vector3d rotationPoint() {
        return this.rotationPoint;
    }

    @Override
    public Vector3d scale() {
        return this.scale;
    }

    public Pose3d set(final Pose3dc other) {
        this.position.set(other.position());
        this.orientation.set(other.orientation());
        this.rotationPoint.set(other.rotationPoint());
        this.scale.set(other.scale());
        return this;
    }

    @Override
    public Pose3d invert() {
        final Quaterniond inverse = new Quaterniond(this.orientation).conjugate();
        final Vector3d inversePosition = new Vector3d(this.position).negate();
        inverse.transform(inversePosition);
        return new Pose3d(inversePosition, inverse, this.rotationPoint,
                new Vector3d(1.0 / this.scale.x, 1.0 / this.scale.y, 1.0 / this.scale.z));
    }
}
