package dev.simulated_team.simulated.backport.physics.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * A cached view over a level, so a flood fill does not pay for a chunk lookup
 * per block.
 *
 * <p>Real, not a stand-in: Sable's version caches the last chunk it touched and
 * reads block states out of it, which is ordinary Minecraft and works here
 * unchanged. What it does <em>not</em> do here is see into sub-levels; there are
 * none until V2, and the callers — the Absorber's enclosure search and the Auger
 * Shaft's neighbour walk — read world blocks either way.
 */
public final class LevelAccelerator {

    private final Level level;

    private ChunkAccess cachedChunk;
    private int cachedChunkX = Integer.MIN_VALUE;
    private int cachedChunkZ = Integer.MIN_VALUE;

    public LevelAccelerator(final Level level) {
        this.level = level;
    }

    public Level level() {
        return this.level;
    }

    public BlockState getBlockState(final BlockPos pos) {
        final int chunkX = pos.getX() >> 4;
        final int chunkZ = pos.getZ() >> 4;

        if (this.cachedChunk == null || chunkX != this.cachedChunkX || chunkZ != this.cachedChunkZ) {
            this.cachedChunk = this.level.getChunk(chunkX, chunkZ);
            this.cachedChunkX = chunkX;
            this.cachedChunkZ = chunkZ;
        }

        return this.cachedChunk.getBlockState(pos);
    }

    public int getMaxBuildHeight() {
        return this.level.getMaxBuildHeight();
    }

    public int getMinBuildHeight() {
        return this.level.getMinBuildHeight();
    }

    public void clearCache() {
        this.cachedChunk = null;
        this.cachedChunkX = Integer.MIN_VALUE;
        this.cachedChunkZ = Integer.MIN_VALUE;
    }
}
