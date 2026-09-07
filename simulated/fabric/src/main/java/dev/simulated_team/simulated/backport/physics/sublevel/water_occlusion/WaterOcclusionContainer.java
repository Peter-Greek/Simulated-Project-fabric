package dev.simulated_team.simulated.backport.physics.sublevel.water_occlusion;

/** Tracks which regions a sub-level keeps dry. Empty here. */
public class WaterOcclusionContainer<T> {

    private static final WaterOcclusionContainer EMPTY = new WaterOcclusionContainer();

    public static WaterOcclusionContainer getContainer(final net.minecraft.world.level.Level level) {
        return EMPTY;
    }

    public void addRegion(@javax.annotation.Nullable final WaterOcclusionRegion region) {
    }

    public void removeRegion(@javax.annotation.Nullable final WaterOcclusionRegion region) {
    }
}
