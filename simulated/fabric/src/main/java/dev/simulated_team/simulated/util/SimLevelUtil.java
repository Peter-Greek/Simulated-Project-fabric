package dev.simulated_team.simulated.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkSource;

/**
 * Deviations from upstream, both landing in V2 with Sable:
 *
 * <ul>
 *   <li>Upstream short-circuits to loaded when the position is inside a
 *       sub-level, because a sub-level's blocks are always resident. There are
 *       no sub-levels here yet.</li>
 *   <li>The NeoForge {@code ILevelReaderExtension#isAreaLoaded} call is the
 *       vanilla chunk check spelled out, which is what that extension does.</li>
 * </ul>
 */
public class SimLevelUtil {
    public static boolean isAreaActuallyLoaded(final Level level, final BlockPos center, final int range) {
        if (!level.hasChunksAt(center.offset(-range, -range, -range), center.offset(range, range, range))) {
            return false;
        } else {
            if (level.isClientSide) {
                final int minY = center.getY() - range;
                final int maxY = center.getY() + range;
                if (maxY < level.getMinBuildHeight() || minY >= level.getMaxBuildHeight()) {
                    return false;
                }

                final int minX = center.getX() - range;
                final int minZ = center.getZ() - range;
                final int maxX = center.getX() + range;
                final int maxZ = center.getZ() + range;
                final int minChunkX = SectionPos.blockToSectionCoord(minX);
                final int maxChunkX = SectionPos.blockToSectionCoord(maxX);
                final int minChunkZ = SectionPos.blockToSectionCoord(minZ);
                final int maxChunkZ = SectionPos.blockToSectionCoord(maxZ);
                final ChunkSource chunkSource = level.getChunkSource();

                for (int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX) {
                    for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ) {
                        if (!chunkSource.hasChunk(chunkX, chunkZ)) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }
    }

}
