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
 * First 1.20.1 backport of Simulated's structure-discovery stage.
 *
 * This deliberately stops before moving blocks into a Sable sub-level. It lets
 * us validate Create movement checks, super glue and sticky attachments against
 * the Homestead stack independently from the physics backport.
 */
public final class FabricAssemblyScanner {
    private static final int MAX_BLOCKS = 128_000;

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

            for (final Direction direction : Direction.values()) {
                final BlockPos neighborPos = pos.relative(direction);
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

                if (isConnected(level, pos, state, neighborPos, neighborState, direction, glueCache)) {
                    frontier.add(neighborPos);
                    queued.add(neighborPos);
                }
            }
        }

        return ScanResult.success(blocks, min, max);
    }

    private static boolean isConnected(final Level level,
                                       final BlockPos pos,
                                       final BlockState state,
                                       final BlockPos neighborPos,
                                       final BlockState neighborState,
                                       final Direction direction,
                                       final Set<SuperGlueEntity> glueCache) {
        if (SuperGlueEntity.isGlued(level, pos, direction, glueCache)) {
            return true;
        }

        if (BlockMovementChecks.isBlockAttachedTowards(state, level, pos, direction)
                || BlockMovementChecks.isBlockAttachedTowards(
                        neighborState, level, neighborPos, direction.getOpposite())) {
            return true;
        }

        if (isSlimeHoneyPair(state, neighborState)) {
            return false;
        }

        final boolean stickyFace = SuperGlueEntity.isSideSticky(level, pos, direction)
                || SuperGlueEntity.isSideSticky(level, neighborPos, direction.getOpposite());
        if (!stickyFace) {
            return false;
        }

        return !BlockMovementChecks.isNotSupportive(state, direction)
                && !BlockMovementChecks.isNotSupportive(neighborState, direction.getOpposite());
    }

    private static boolean isSlimeHoneyPair(final BlockState first, final BlockState second) {
        return first.is(Blocks.SLIME_BLOCK) && second.is(Blocks.HONEY_BLOCK)
                || first.is(Blocks.HONEY_BLOCK) && second.is(Blocks.SLIME_BLOCK);
    }

    public record ScanResult(boolean successful,
                             Set<BlockPos> blocks,
                             BlockPos min,
                             BlockPos max,
                             String error,
                             BlockPos problemPos) {
        private static ScanResult success(final Set<BlockPos> blocks, final BlockPos min, final BlockPos max) {
            return new ScanResult(true, Set.copyOf(blocks), min, max, null, null);
        }

        private static ScanResult failure(final String error, final BlockPos problemPos) {
            return new ScanResult(false, Set.of(), null, null, error, problemPos.immutable());
        }
    }
}
