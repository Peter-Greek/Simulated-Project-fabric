package dev.simulated_team.simulated.backport.physics.sublevel.plot;

import dev.simulated_team.simulated.backport.physics.companion.math.BoundingBox3i;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import java.util.Collections;
import java.util.Set;

/**
 * Stand-in for Sable's {@code LevelPlot}. Nothing on this stack constructs
 * one — see the package documentation — so this exists to keep upstream's
 * declarations compiling until V2 brings the engine in.
 */
public class LevelPlot {

    public BoundingBox3i getBoundingBox() {
        return BoundingBox3i.EMPTY;
    }

    public ChunkPos getCenterChunk() {
        return ChunkPos.ZERO;
    }

    public void newEmptyChunk(final ChunkPos pos) {
    }

    /** Write access into a plot's blocks. No plot exists, so there is none. */
    @javax.annotation.Nullable
    public net.minecraft.world.level.LevelAccessor getEmbeddedLevelAccessor() {
        return null;
    }

    public BlockPos getCenterBlock() {
        return BlockPos.ZERO;
    }

    public Set<ChunkPos> getLoadedChunks() {
        return Collections.emptySet();
    }
}
