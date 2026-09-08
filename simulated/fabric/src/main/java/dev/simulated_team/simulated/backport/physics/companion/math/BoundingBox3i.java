package dev.simulated_team.simulated.backport.physics.companion.math;

import net.minecraft.core.Vec3i;

import java.util.Collection;

/** Axis-aligned integer box. Upstream exposes the bounds as public fields. */
public class BoundingBox3i implements BoundingBox3ic {

    /** Upstream's sentinel for "no blocks at all". */
    public static final BoundingBox3i EMPTY = new BoundingBox3i(0, 0, 0, -1, -1, -1);

    public int minX;
    public int minY;
    public int minZ;
    public int maxX;
    public int maxY;
    public int maxZ;

    public BoundingBox3i() {
    }

    public BoundingBox3i(final int minX, final int minY, final int minZ,
                         final int maxX, final int maxY, final int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public BoundingBox3i(final BoundingBox3ic other) {
        this(other.minX(), other.minY(), other.minZ(), other.maxX(), other.maxY(), other.maxZ());
    }

    /** The smallest box containing every given position, or {@link #EMPTY}. */
    public static BoundingBox3i from(final Collection<? extends Vec3i> positions) {
        if (positions.isEmpty()) {
            return EMPTY;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (final Vec3i pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new BoundingBox3i(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean contains(final int x, final int y, final int z) {
        return x >= this.minX && x <= this.maxX
                && y >= this.minY && y <= this.maxY
                && z >= this.minZ && z <= this.maxZ;
    }

    @Override
    public int minX() {
        return this.minX;
    }

    @Override
    public int minY() {
        return this.minY;
    }

    @Override
    public int minZ() {
        return this.minZ;
    }

    @Override
    public int maxX() {
        return this.maxX;
    }

    @Override
    public int maxY() {
        return this.maxY;
    }

    @Override
    public int maxZ() {
        return this.maxZ;
    }
}
