package dev.simulated_team.simulated.backport.physics.companion.math;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Read-only view of a rigid transform: a translation, an orientation, and the
 * point the orientation turns about.
 *
 * <p>Pure geometry, so this is a real implementation. In V1 every pose that
 * reaches a ported file is the identity, because nothing is inside a sub-level.
 */
public interface Pose3dc {

    Vector3dc position();

    Quaterniond orientation();

    Vector3dc rotationPoint();

    Vector3dc scale();

    default Vector3d transformPosition(final Vector3dc point) {
        return this.transformPosition(point, new Vector3d());
    }

    default Vector3d transformPosition(final Vector3dc point, final Vector3d destination) {
        destination.set(point).sub(this.rotationPoint()).mul(this.scale());
        this.orientation().transform(destination);
        return destination.add(this.rotationPoint()).add(this.position());
    }

    default Vec3 transformPosition(final Vec3 point) {
        final Vector3d transformed = this.transformPosition(
                new Vector3d(point.x, point.y, point.z), new Vector3d());
        return new Vec3(transformed.x, transformed.y, transformed.z);
    }

    default Vector3d transformPositionInverse(final Vector3dc point) {
        return this.transformPositionInverse(point, new Vector3d());
    }

    default Vector3d transformPositionInverse(final Vector3dc point, final Vector3d destination) {
        destination.set(point).sub(this.position()).sub(this.rotationPoint());
        this.orientation().transformInverse(destination);
        final Vector3dc scale = this.scale();
        destination.set(destination.x / scale.x(), destination.y / scale.y(), destination.z / scale.z());
        return destination.add(this.rotationPoint());
    }

    default Vec3 transformPositionInverse(final Vec3 point) {
        final Vector3d transformed = this.transformPositionInverse(
                new Vector3d(point.x, point.y, point.z), new Vector3d());
        return new Vec3(transformed.x, transformed.y, transformed.z);
    }

    /** A direction carries no translation, so only the orientation applies. */
    default Vector3d transformNormal(final Vector3dc normal) {
        return this.transformNormal(normal, new Vector3d());
    }

    default Vector3d transformNormal(final Vector3dc normal, final Vector3d destination) {
        destination.set(normal);
        return this.orientation().transform(destination);
    }

    default Vec3 transformNormal(final Vec3 normal) {
        final Vector3d transformed = this.transformNormal(
                new Vector3d(normal.x, normal.y, normal.z), new Vector3d());
        return new Vec3(transformed.x, transformed.y, transformed.z);
    }

    default Vector3d transformNormalInverse(final Vector3dc normal) {
        return this.transformNormalInverse(normal, new Vector3d());
    }

    default Vector3d transformNormalInverse(final Vector3dc normal, final Vector3d destination) {
        destination.set(normal);
        return this.orientation().transformInverse(destination);
    }

    default Vec3 transformNormalInverse(final Vec3 normal) {
        final Vector3d transformed = this.transformNormalInverse(
                new Vector3d(normal.x, normal.y, normal.z), new Vector3d());
        return new Vec3(transformed.x, transformed.y, transformed.z);
    }

    /** Upstream's name for {@link #transformNormal}. */
    default Vector3d transformDirection(final Vector3dc direction) {
        return this.transformNormal(direction);
    }

    default Vec3 transformDirection(final Vec3 direction) {
        return this.transformNormal(direction);
    }

    Pose3d invert();
}
