package dev.simulated_team.simulated.backport.physics.companion.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector3dc;

/**
 * Axis-aligned double-precision box. Upstream exposes the bounds as public
 * fields, so they stay fields here.
 */
public class BoundingBox3d implements BoundingBox3dc {

    public static final Codec<BoundingBox3d> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("minX").forGetter(box -> box.minX),
            Codec.DOUBLE.fieldOf("minY").forGetter(box -> box.minY),
            Codec.DOUBLE.fieldOf("minZ").forGetter(box -> box.minZ),
            Codec.DOUBLE.fieldOf("maxX").forGetter(box -> box.maxX),
            Codec.DOUBLE.fieldOf("maxY").forGetter(box -> box.maxY),
            Codec.DOUBLE.fieldOf("maxZ").forGetter(box -> box.maxZ)
    ).apply(instance, BoundingBox3d::new));

    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;

    public BoundingBox3d() {
    }

    public BoundingBox3d(final double minX, final double minY, final double minZ,
                         final double maxX, final double maxY, final double maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public BoundingBox3d(final BoundingBox3dc other) {
        this(other.minX(), other.minY(), other.minZ(), other.maxX(), other.maxY(), other.maxZ());
    }

    public BoundingBox3d(final Vector3dc min, final Vector3dc max) {
        this(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
    }

    public BoundingBox3d set(final double minX, final double minY, final double minZ,
                             final double maxX, final double maxY, final double maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        return this;
    }

    @Override
    public double minX() {
        return this.minX;
    }

    @Override
    public double minY() {
        return this.minY;
    }

    @Override
    public double minZ() {
        return this.minZ;
    }

    @Override
    public double maxX() {
        return this.maxX;
    }

    @Override
    public double maxY() {
        return this.maxY;
    }

    @Override
    public double maxZ() {
        return this.maxZ;
    }

    @Override
    public String toString() {
        return "BoundingBox3d[" + this.minX + ", " + this.minY + ", " + this.minZ
                + " -> " + this.maxX + ", " + this.maxY + ", " + this.maxZ + "]";
    }
}
