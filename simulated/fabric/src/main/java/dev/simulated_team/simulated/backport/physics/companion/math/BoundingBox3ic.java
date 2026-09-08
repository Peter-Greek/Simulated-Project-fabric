package dev.simulated_team.simulated.backport.physics.companion.math;

/** Read-only view of an axis-aligned integer box. Pure geometry. */
public interface BoundingBox3ic {

    int minX();

    int minY();

    int minZ();

    int maxX();

    int maxY();

    int maxZ();

    default boolean contains(final int x, final int y, final int z) {
        return x >= this.minX() && x <= this.maxX()
                && y >= this.minY() && y <= this.maxY()
                && z >= this.minZ() && z <= this.maxZ();
    }
}
