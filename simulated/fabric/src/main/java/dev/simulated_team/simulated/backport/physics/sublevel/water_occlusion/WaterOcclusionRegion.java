package dev.simulated_team.simulated.backport.physics.sublevel.water_occlusion;

/** One dry region inside a sub-level. Never created here. */
public class WaterOcclusionRegion {

    /** The region enclosing these blocks. Nothing keeps water out here. */
    @javax.annotation.Nullable
    public static WaterOcclusionRegion fromBlocks(final java.util.Collection<? extends net.minecraft.core.BlockPos> blocks) {
        return null;
    }

    /** Whether the region has changed since it was last applied. */
    public boolean isDirty() {
        return false;
    }
}
