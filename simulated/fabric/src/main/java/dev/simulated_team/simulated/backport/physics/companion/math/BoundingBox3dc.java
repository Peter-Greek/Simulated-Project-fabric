package dev.simulated_team.simulated.backport.physics.companion.math;

import org.joml.Vector3d;

/** Read-only view of an axis-aligned double-precision box. Pure geometry. */
public interface BoundingBox3dc {

    double minX();

    double minY();

    double minZ();

    double maxX();

    double maxY();

    double maxZ();

    default double sizeX() {
        return this.maxX() - this.minX();
    }

    default double sizeY() {
        return this.maxY() - this.minY();
    }

    default double sizeZ() {
        return this.maxZ() - this.minZ();
    }

    default Vector3d center() {
        return new Vector3d(
                (this.minX() + this.maxX()) * 0.5,
                (this.minY() + this.maxY()) * 0.5,
                (this.minZ() + this.maxZ()) * 0.5);
    }

    default boolean contains(final double x, final double y, final double z) {
        return x >= this.minX() && x <= this.maxX()
                && y >= this.minY() && y <= this.maxY()
                && z >= this.minZ() && z <= this.maxZ();
    }

    default boolean intersects(final BoundingBox3dc other) {
        return this.minX() <= other.maxX() && this.maxX() >= other.minX()
                && this.minY() <= other.maxY() && this.maxY() >= other.minY()
                && this.minZ() <= other.maxZ() && this.maxZ() >= other.minZ();
    }
}
