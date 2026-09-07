package dev.simulated_team.simulated.content.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;

/**
 * The End Sea world preset: the player starts in the End, above the void sea.
 *
 * <p>Two deviations from upstream, both recorded in FABRIC_PORT_PLAN.md:
 *
 * <ul>
 *   <li>The dimension change uses 1.20.1's {@code changeDimension}. 1.21's
 *       {@code DimensionTransition} does not exist here; the teleport that
 *       follows puts the player in the same place either way.</li>
 *   <li>Upstream also assembles a starting platform as a <em>sub-level</em>, so
 *       the player begins on a floating physics body. There are no sub-levels
 *       until V2, so the platform is placed as ordinary world blocks at the same
 *       position instead — the player still starts on solid ground over the
 *       sea.</li>
 * </ul>
 */
public class EndSeaPreset extends SimulatedWorldPreset {

    public static Vec3 PLAYER_SPAWN_POS = new Vec3(0, -30, 0);

    /** Where upstream's sub-level platform sits, in world coordinates. */
    private static final BlockPos PLATFORM_ORIGIN = BlockPos.containing(-4.5, -41.0, -4.5);

    private static final int PLATFORM_RADIUS = 5;

    public EndSeaPreset(final ResourceLocation id, final Component description) {
        super(id, description);
    }

    @Override
    public void onPlayerJoin(final ServerLevel level, final ServerPlayer player) {
        if (!level.dimension().equals(Level.END)) {
            player.setRespawnPosition(Level.END, BlockPos.containing(PLAYER_SPAWN_POS), 0.0f, true, false);

            final ServerLevel endLevel = level.getServer().getLevel(Level.END);
            if (endLevel != null) {
                player.changeDimension(endLevel);
            }
            player.teleportTo(PLAYER_SPAWN_POS.x(), PLAYER_SPAWN_POS.y(), PLAYER_SPAWN_POS.z());
        }
    }

    @Override
    public void onChunkLoad(final ServerLevel level, final ChunkAccess chunkAccess, final boolean newChunk) {
        if (!newChunk || !chunkAccess.getPos().equals(ChunkPos.ZERO) || !level.dimension().equals(Level.END)) {
            return;
        }

        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = -PLATFORM_RADIUS; i < PLATFORM_RADIUS; i++) {
            for (int j = -PLATFORM_RADIUS; j < PLATFORM_RADIUS; j++) {
                pos.set(PLATFORM_ORIGIN.getX() + i, PLATFORM_ORIGIN.getY(), PLATFORM_ORIGIN.getZ() + j);
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }
}
