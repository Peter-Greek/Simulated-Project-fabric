package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Minecraft 1.20.1 backport of Simulated's structure-discovery stage.
 *
 * This deliberately stops before moving blocks into a Sable sub-level. It lets
 * us validate Create movement checks, Super Glue and sticky attachments against
 * the Homestead stack independently from the physics backport.
 */
public final class FabricAssemblyScanner {
    private static final int MAX_BLOCKS = 128_000;

    /**
     * Matches the neighborhood used by upstream SimAssemblyContraption. The
     * twelve edge-diagonal offsets matter for Super Glue sheets: one glue entity
     * can contain blocks which are not face-adjacent to the block currently
     * being visited.
     */
    private static final BlockPos[] DIRECTION_OFFSETS = new BlockPos[]{
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0),
            new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1),
            new BlockPos(1, 1, 0),
            new BlockPos(-1, -1, 0),
            new BlockPos(1, -1, 0),
            new BlockPos(-1, 1, 0),
            new BlockPos(1, 0, 1),
            new BlockPos(-1, 0, -1),
            new BlockPos(1, 0, -1),
            new BlockPos(-1, 0, 1),
            new BlockPos(0, 1, 1),
            new BlockPos(0, -1, -1),
            new BlockPos(0, -1, 1),
            new BlockPos(0, 1, -1)
    };

    private FabricAssemblyScanner() {
    }

    public static ScanResult scan(final Level level, final BlockPos assemblerPos, final BlockPos startPos) {
        final BlockState startState = level.getBlockState(startPos);
        if (startState.isAir()) {
            return ScanResult.failure("No block is attached to the Physics Assembler's sticky face.", startPos);
        }

        final Queue<BlockPos> frontier = new ArrayDeque<>();
        final Set<BlockPos> queued = new HashSet<>();
        final LinkedHashSet<BlockPos> blocks = new LinkedHashSet<>();
        final Set<SuperGlueEntity> glueCache = new HashSet<>();

        frontier.add(startPos);
        queued.add(startPos);

        BlockPos min = startPos;
        BlockPos max = startPos;

        while (!frontier.isEmpty()) {
            final BlockPos pos = frontier.remove();
            if (pos.equals(assemblerPos)) {
                continue;
            }

            if (level.isOutsideBuildHeight(pos)) {
                continue;
            }

            if (!level.isLoaded(pos)) {
                return ScanResult.failure("Assembly reaches an unloaded chunk.", pos);
            }

            final BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            if (!BlockMovementChecks.isMovementAllowed(state, level, pos)) {
                return ScanResult.failure("Assembly contains a block Create considers immovable.", pos);
            }

            blocks.add(pos.immutable());
            if (blocks.size() > MAX_BLOCKS) {
                return ScanResult.failure("Assembly is larger than the current 128,000 block limit.", pos);
            }

            min = new BlockPos(
                    Math.min(min.getX(), pos.getX()),
                    Math.min(min.getY(), pos.getY()),
                    Math.min(min.getZ(), pos.getZ()));
            max = new BlockPos(
                    Math.max(max.getX(), pos.getX()),
                    Math.max(max.getY(), pos.getY()),
                    Math.max(max.getZ(), pos.getZ()));

            for (final BlockPos offset : DIRECTION_OFFSETS) {
                final BlockPos neighborPos = pos.offset(offset);
                if (neighborPos.equals(assemblerPos) || queued.contains(neighborPos)) {
                    continue;
                }

                if (level.isOutsideBuildHeight(neighborPos)) {
                    continue;
                }

                if (!level.isLoaded(neighborPos)) {
                    continue;
                }

                final BlockState neighborState = level.getBlockState(neighborPos);
                if (neighborState.isAir()) {
                    continue;
                }

                final int distance = Math.abs(offset.getX()) + Math.abs(offset.getY()) + Math.abs(offset.getZ());
                final Direction cardinalDirection = distance == 1
                        ? Direction.fromDelta(offset.getX(), offset.getY(), offset.getZ())
                        : null;

                if (isConnected(level, pos, state, neighborPos, neighborState, cardinalDirection, glueCache)) {
                    frontier.add(neighborPos);
                    queued.add(neighborPos);
                }
            }
        }

        return ScanResult.success(blocks, glueCache, min, max);
    }

    private static boolean isConnected(final Level level,
                                       final BlockPos pos,
                                       final BlockState state,
                                       final BlockPos neighborPos,
                                       final BlockState neighborState,
                                       final Direction cardinalDirection,
                                       final Set<SuperGlueEntity> glueCache) {
        // Upstream Simulated checks whether one Super Glue entity contains both
        // positions. This works for direct neighbors and for edge-diagonal blocks
        // covered by the same glue sheet.
        if (isGluedBetween(level, pos, neighborPos, glueCache)) {
            return true;
        }

        // The remaining Create attachment rules are face-directional and do not
        // apply to the diagonal offsets above.
        if (cardinalDirection == null) {
            return false;
        }

        if (BlockMovementChecks.isBlockAttachedTowards(state, level, pos, cardinalDirection)
                || BlockMovementChecks.isBlockAttachedTowards(
                        neighborState, level, neighborPos, cardinalDirection.getOpposite())) {
            return true;
        }

        if (isSlimeHoneyPair(state, neighborState)) {
            return false;
        }

        final boolean stickyFace = SuperGlueEntity.isSideSticky(level, pos, cardinalDirection)
                || SuperGlueEntity.isSideSticky(level, neighborPos, cardinalDirection.getOpposite());
        if (!stickyFace) {
            return false;
        }

        return !BlockMovementChecks.isNotSupportive(state, cardinalDirection)
                && !BlockMovementChecks.isNotSupportive(neighborState, cardinalDirection.getOpposite());
    }

    private static boolean isGluedBetween(final Level level,
                                          final BlockPos first,
                                          final BlockPos second,
                                          final Set<SuperGlueEntity> glueCache) {
        for (final SuperGlueEntity glue : glueCache) {
            if (glue.contains(first) && glue.contains(second)) {
                return true;
            }
        }

        for (final SuperGlueEntity glue : level.getEntitiesOfClass(
                SuperGlueEntity.class,
                SuperGlueEntity.span(first, second).inflate(16))) {
            if (!glue.contains(first) || !glue.contains(second)) {
                continue;
            }

            glueCache.add(glue);
            return true;
        }

        return false;
    }

    private static boolean isSlimeHoneyPair(final BlockState first, final BlockState second) {
        return first.is(Blocks.SLIME_BLOCK) && second.is(Blocks.HONEY_BLOCK)
                || first.is(Blocks.HONEY_BLOCK) && second.is(Blocks.SLIME_BLOCK);
    }

    public record ScanResult(boolean successful,
                             Set<BlockPos> blocks,
                             Set<SuperGlueEntity> glues,
                             BlockPos min,
                             BlockPos max,
                             String error,
                             BlockPos problemPos) {
        private static ScanResult success(final Set<BlockPos> blocks,
                                          final Set<SuperGlueEntity> glues,
                                          final BlockPos min,
                                          final BlockPos max) {
            return new ScanResult(true, Set.copyOf(blocks), Set.copyOf(glues), min, max, null, null);
        }

        private static ScanResult failure(final String error, final BlockPos problemPos) {
            return new ScanResult(false, Set.of(), Set.of(), null, null, error, problemPos.immutable());
        }
    }
}
