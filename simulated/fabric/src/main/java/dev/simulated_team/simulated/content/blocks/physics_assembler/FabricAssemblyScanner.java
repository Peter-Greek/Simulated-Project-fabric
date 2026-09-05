package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.contraptions.chassis.AbstractChassisBlock;
import com.simibubi.create.content.contraptions.chassis.ChassisBlockEntity;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonHeadBlock;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Minecraft 1.20.1 backport of Simulated's structure-discovery stage.
 *
 * This deliberately stops before moving blocks into a Sable sub-level. It lets
 * us validate Create movement checks, Super Glue and Create-specific attachment
 * rules against the Homestead stack independently from the physics backport.
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

        // Upstream SimAssemblyContraption does not enqueue a brittle seed. A
        // brittle block can still be carried later when it is explicitly glued
        // or otherwise attached; that distinction matters for carpet/torch tests.
        if (BlockMovementChecks.isBrittle(startState)) {
            return ScanResult.failure("The block on the Physics Assembler's sticky face is brittle and cannot seed an assembly.", startPos);
        }

        final Queue<BlockPos> frontier = new ArrayDeque<>();
        final Set<BlockPos> queued = new HashSet<>();
        final LinkedHashSet<BlockPos> blocks = new LinkedHashSet<>();
        final Set<SuperGlueEntity> glueCache = new HashSet<>();
        final MutableStats stats = new MutableStats();

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

            if (!blocks.add(pos.immutable())) {
                continue;
            }
            if (BlockMovementChecks.isBrittle(state)) {
                stats.brittleBlocks++;
            }
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

            // Vanilla double-chest halves are forced to travel together by
            // upstream Simulated even if there is no Super Glue on their seam.
            if (state.hasProperty(ChestBlock.TYPE)
                    && state.hasProperty(ChestBlock.FACING)
                    && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                final BlockPos attached = pos.relative(ChestBlock.getConnectedDirection(state));
                if (enqueue(attached, assemblerPos, frontier, queued)) {
                    stats.chestLinks++;
                }
            }

            // Bogeys expose their own sticky surfaces and are handled explicitly
            // by upstream before the generic glue/sticky search.
            if (state.getBlock() instanceof final AbstractBogeyBlock<?> bogey) {
                for (final Direction direction : bogey.getStickySurfaces(level, pos, state)) {
                    if (enqueue(pos.relative(direction), assemblerPos, frontier, queued)) {
                        stats.bogeyLinks++;
                    }
                }
            }

            // Chassis ranges/groups must be honored even when blocks are not
            // reachable through ordinary face-stickiness.
            if (state.getBlock() instanceof AbstractChassisBlock
                    && level.getBlockEntity(pos) instanceof final ChassisBlockEntity chassis) {
                final Queue<BlockPos> chassisFrontier = new ArrayDeque<>();
                final Set<BlockPos> chassisVisited = new HashSet<>(queued);
                chassis.addAttachedChasses(chassisFrontier, chassisVisited);
                for (final BlockPos chassisPos : chassisFrontier) {
                    if (enqueue(chassisPos, assemblerPos, frontier, queued)) {
                        stats.chassisLinks++;
                    }
                }
                final var included = chassis.getIncludedBlockPositions(null, false);
                if (included == null) {
                    return ScanResult.failure("Create chassis could not resolve its attached block range.", pos);
                }
                for (final BlockPos includedPos : included) {
                    if (enqueue(includedPos, assemblerPos, frontier, queued)) {
                        stats.chassisLinks++;
                    }
                }
            }

            // Mechanical pistons, poles and heads are structurally linked in
            // ways generic glue rules do not describe. These cases mirror the
            // upstream SimAssemblyContraption helpers.
            if (state.getBlock() instanceof MechanicalPistonBlock) {
                final Direction pistonFacing = state.getValue(MechanicalPistonBlock.FACING);
                final PistonState pistonState = state.getValue(MechanicalPistonBlock.STATE);
                if (pistonState == PistonState.MOVING) {
                    return ScanResult.failure("Assembly contains a mechanical piston while it is moving.", pos);
                }

                final BlockPos behind = pos.relative(pistonFacing.getOpposite());
                final BlockState behindState = level.getBlockState(behind);
                if (MechanicalPistonBlock.isExtensionPole(behindState)
                        && behindState.getValue(PistonExtensionPoleBlock.FACING).getAxis() == pistonFacing.getAxis()
                        && enqueue(behind, assemblerPos, frontier, queued)) {
                    stats.pistonLinks++;
                }

                if ((pistonState == PistonState.EXTENDED || MechanicalPistonBlock.isStickyPiston(state))
                        && enqueue(pos.relative(pistonFacing), assemblerPos, frontier, queued)) {
                    stats.pistonLinks++;
                }
            } else if (MechanicalPistonBlock.isPistonHead(state)) {
                final Direction headFacing = state.getValue(MechanicalPistonHeadBlock.FACING);
                final BlockPos behind = pos.relative(headFacing.getOpposite());
                final BlockState behindState = level.getBlockState(behind);
                final boolean validPole = MechanicalPistonBlock.isExtensionPole(behindState)
                        && behindState.getValue(PistonExtensionPoleBlock.FACING).getAxis() == headFacing.getAxis();
                final boolean validBase = MechanicalPistonBlock.isPiston(behindState)
                        && behindState.getValue(MechanicalPistonBlock.FACING) == headFacing
                        && behindState.getValue(MechanicalPistonBlock.STATE) == PistonState.EXTENDED;
                if ((validPole || validBase) && enqueue(behind, assemblerPos, frontier, queued)) {
                    stats.pistonLinks++;
                }
                if (state.getValue(MechanicalPistonHeadBlock.TYPE) == PistonType.STICKY
                        && enqueue(pos.relative(headFacing), assemblerPos, frontier, queued)) {
                    stats.pistonLinks++;
                }
            } else if (MechanicalPistonBlock.isExtensionPole(state)) {
                final Direction.Axis poleAxis = state.getValue(PistonExtensionPoleBlock.FACING).getAxis();
                for (final Direction direction : Iterate.directionsInAxis(poleAxis)) {
                    final BlockPos attached = pos.relative(direction);
                    final BlockState attachedState = level.getBlockState(attached);
                    final boolean validPole = MechanicalPistonBlock.isExtensionPole(attachedState)
                            && attachedState.getValue(PistonExtensionPoleBlock.FACING).getAxis() == poleAxis;
                    final boolean validHead = MechanicalPistonBlock.isPistonHead(attachedState)
                            && attachedState.getValue(MechanicalPistonHeadBlock.FACING).getAxis() == poleAxis;
                    final boolean validBase = MechanicalPistonBlock.isPiston(attachedState)
                            && attachedState.getValue(MechanicalPistonBlock.FACING).getAxis() == poleAxis
                            && attachedState.getValue(MechanicalPistonBlock.STATE) == PistonState.EXTENDED;
                    if ((validPole || validHead || validBase)
                            && enqueue(attached, assemblerPos, frontier, queued)) {
                        stats.pistonLinks++;
                    }
                }
            }

            // Gantry pinions and shafts also form structural chains independently
            // of Super Glue.
            if (state.getBlock() instanceof final GantryCarriageBlock carriage) {
                final Direction carriageFacing = state.getValue(GantryCarriageBlock.FACING);
                if (enqueue(pos.relative(carriageFacing), assemblerPos, frontier, queued)) {
                    stats.gantryLinks++;
                }
                final Direction.Axis rotationAxis = ((IRotate) carriage).getRotationAxis(state);
                for (final Direction direction : Iterate.directionsInAxis(rotationAxis)) {
                    final BlockPos attached = pos.relative(direction);
                    final BlockState attachedState = level.getBlockState(attached);
                    if (AllBlocks.GANTRY_SHAFT.has(attachedState)
                            && attachedState.getValue(GantryShaftBlock.FACING).getAxis() == direction.getAxis()
                            && enqueue(attached, assemblerPos, frontier, queued)) {
                        stats.gantryLinks++;
                    }
                }
            } else if (state.getBlock() instanceof GantryShaftBlock) {
                final Direction shaftFacing = state.getValue(GantryShaftBlock.FACING);
                for (final Direction direction : Iterate.directions) {
                    final BlockPos attached = pos.relative(direction);
                    final BlockState attachedState = level.getBlockState(attached);
                    final boolean sameShaft = direction.getAxis() == shaftFacing.getAxis()
                            && AllBlocks.GANTRY_SHAFT.has(attachedState)
                            && attachedState.getValue(GantryShaftBlock.FACING) == shaftFacing;
                    final boolean carriageFacingShaft = AllBlocks.GANTRY_CARRIAGE.has(attachedState)
                            && attachedState.getValue(GantryCarriageBlock.FACING) == direction;
                    if ((sameShaft || carriageFacingShaft)
                            && enqueue(attached, assemblerPos, frontier, queued)) {
                        stats.gantryLinks++;
                    }
                }
            }

            // Create cart assemblers attach themselves to a structure directly
            // above them; this mirrors the special-case in SimAssemblyContraption.
            final BlockPos below = pos.below();
            if (AllBlocks.CART_ASSEMBLER.has(level.getBlockState(below))
                    && enqueue(below, assemblerPos, frontier, queued)) {
                stats.cartAssemblerLinks++;
            }

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

                final ConnectionType connection = connectionType(
                        level, pos, state, neighborPos, neighborState, cardinalDirection, glueCache);
                if (connection != ConnectionType.NONE && enqueue(neighborPos, assemblerPos, frontier, queued)) {
                    if (connection == ConnectionType.GLUE) {
                        stats.glueLinks++;
                        if (BlockMovementChecks.isBrittle(neighborState)) {
                            stats.glueCarriedBrittle++;
                        }
                    } else if (connection == ConnectionType.ATTACHMENT) {
                        stats.createAttachmentLinks++;
                    } else if (connection == ConnectionType.STICKY) {
                        stats.stickyLinks++;
                    }
                }
            }
        }

        return ScanResult.success(blocks, glueCache, min, max, stats.freeze());
    }

    private static boolean enqueue(final BlockPos pos,
                                   final BlockPos assemblerPos,
                                   final Queue<BlockPos> frontier,
                                   final Set<BlockPos> queued) {
        if (pos.equals(assemblerPos) || !queued.add(pos)) {
            return false;
        }
        frontier.add(pos);
        return true;
    }

    private static ConnectionType connectionType(final Level level,
                                                 final BlockPos pos,
                                                 final BlockState state,
                                                 final BlockPos neighborPos,
                                                 final BlockState neighborState,
                                                 final Direction cardinalDirection,
                                                 final Set<SuperGlueEntity> glueCache) {
        // Explicit glue is allowed to carry brittle blocks in upstream Simulated.
        // This is why a glued carpet counts even though a carpet cannot seed an
        // assembly by itself.
        if (isGluedBetween(level, pos, neighborPos, glueCache)) {
            return ConnectionType.GLUE;
        }

        // The remaining Create attachment rules are face-directional and do not
        // apply to the diagonal offsets above.
        if (cardinalDirection == null) {
            return ConnectionType.NONE;
        }

        if (BlockMovementChecks.isBlockAttachedTowards(state, level, pos, cardinalDirection)
                || BlockMovementChecks.isBlockAttachedTowards(
                        neighborState, level, neighborPos, cardinalDirection.getOpposite())) {
            return ConnectionType.ATTACHMENT;
        }

        if (isSlimeHoneyPair(state, neighborState)) {
            return ConnectionType.NONE;
        }

        // Brittle blocks and PUSH_ONLY blocks can still be explicitly glued or
        // attached, but must not be dragged merely by a sticky surface.
        if (BlockMovementChecks.isBrittle(state)
                || BlockMovementChecks.isBrittle(neighborState)
                || state.getPistonPushReaction() == PushReaction.PUSH_ONLY
                || neighborState.getPistonPushReaction() == PushReaction.PUSH_ONLY) {
            return ConnectionType.NONE;
        }

        final boolean stickyFace = SuperGlueEntity.isSideSticky(level, pos, cardinalDirection)
                || SuperGlueEntity.isSideSticky(level, neighborPos, cardinalDirection.getOpposite());
        if (!stickyFace) {
            return ConnectionType.NONE;
        }

        return !BlockMovementChecks.isNotSupportive(state, cardinalDirection)
                && !BlockMovementChecks.isNotSupportive(neighborState, cardinalDirection.getOpposite())
                ? ConnectionType.STICKY
                : ConnectionType.NONE;
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

    private enum ConnectionType {
        NONE,
        GLUE,
        ATTACHMENT,
        STICKY
    }

    private static final class MutableStats {
        private int brittleBlocks;
        private int glueCarriedBrittle;
        private int glueLinks;
        private int createAttachmentLinks;
        private int stickyLinks;
        private int chestLinks;
        private int chassisLinks;
        private int bogeyLinks;
        private int pistonLinks;
        private int gantryLinks;
        private int cartAssemblerLinks;

        private ScanStats freeze() {
            return new ScanStats(
                    brittleBlocks,
                    glueCarriedBrittle,
                    glueLinks,
                    createAttachmentLinks,
                    stickyLinks,
                    chestLinks,
                    chassisLinks,
                    bogeyLinks,
                    pistonLinks,
                    gantryLinks,
                    cartAssemblerLinks);
        }
    }

    public record ScanStats(int brittleBlocks,
                            int glueCarriedBrittle,
                            int glueLinks,
                            int createAttachmentLinks,
                            int stickyLinks,
                            int chestLinks,
                            int chassisLinks,
                            int bogeyLinks,
                            int pistonLinks,
                            int gantryLinks,
                            int cartAssemblerLinks) {
        public static ScanStats empty() {
            return new ScanStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        public String summary() {
            return "brittle=" + brittleBlocks
                    + " (glue-carried=" + glueCarriedBrittle + ")"
                    + ", links[glue=" + glueLinks
                    + ", create=" + createAttachmentLinks
                    + ", sticky=" + stickyLinks
                    + ", chest=" + chestLinks
                    + ", chassis=" + chassisLinks
                    + ", bogey=" + bogeyLinks
                    + ", piston=" + pistonLinks
                    + ", gantry=" + gantryLinks
                    + ", cart=" + cartAssemblerLinks + "]";
        }
    }

    public record ScanResult(boolean successful,
                             Set<BlockPos> blocks,
                             Set<SuperGlueEntity> glues,
                             BlockPos min,
                             BlockPos max,
                             ScanStats stats,
                             String error,
                             BlockPos problemPos) {
        private static ScanResult success(final Set<BlockPos> blocks,
                                          final Set<SuperGlueEntity> glues,
                                          final BlockPos min,
                                          final BlockPos max,
                                          final ScanStats stats) {
            return new ScanResult(true, Set.copyOf(blocks), Set.copyOf(glues), min, max, stats, null, null);
        }

        private static ScanResult failure(final String error, final BlockPos problemPos) {
            return new ScanResult(false, Set.of(), Set.of(), null, null, ScanStats.empty(), error, problemPos.immutable());
        }
    }
}
