package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.simibubi.create.content.contraptions.AssemblyException;
import dev.simulated_team.simulated.content.blocks.physics_assembler.assembly_preventer.DisassemblyPrevention;
import dev.simulated_team.simulated.index.SimBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IControlContraption;
import dev.simulated_team.simulated.fabric.SimulatedFabricContent;
import dev.simulated_team.simulated.fabric.SimulatedFabricNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import net.createmod.catnip.animation.LerpedFloat;

/**
 * Fabric 1.20.1 Physics Assembler controller.
 *
 * Until Sable's sublevel/physics backend is available on 1.20.1, Create's
 * ControlledContraptionEntity provides rendering, storage, movement actors and
 * collision. The visible Physics Assembler now travels inside that contraption;
 * an invisible anchor remains in the parent world only because Create requires
 * an IControlContraption block entity at a stable world position.
 */
public final class PhysicsAssemblerBlockEntity extends BlockEntity implements IControlContraption {

    /** Lever animation state; see {@link #clientFlickLeverTo}. */
    private static final float FLICKED_ANGLE_DEGREES = 45.0f;

    private static final double LEVER_CHASE_SPEED = 0.75f;

    protected final LerpedFloat visualAngle = LerpedFloat.linear();

    private boolean leverInitialized;

    protected boolean holdingLever;

    private boolean controlledByPlayer;

    private float playerAngle;

    public enum AssemblyState {
        IDLE,
        ASSEMBLING,
        ASSEMBLED,
        ERROR
    }

    public record OperationResult(boolean successful, String message, int blockCount) {
        static OperationResult success(final String message, final int blockCount) {
            return new OperationResult(true, message, blockCount);
        }

        static OperationResult failure(final String message) {
            return new OperationResult(false, message, 0);
        }
    }

    private AssemblyState assemblyState = AssemblyState.IDLE;
    private ControlledContraptionEntity movedContraption;
    private UUID movedContraptionId;
    /** Number of attached payload blocks, excluding the Physics Assembler itself. */
    private int activeBlockCount;
    private int interactionCount;
    private String lastError = "";
    /** Scan-rule counters from the most recent assembly attempt, for /simulated assembler_state. */
    private String lastScanSummary = "";

    private long lastProcessedGameTime = Long.MIN_VALUE;
    private UUID lastProcessedPlayer;

    public PhysicsAssemblerBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public boolean tryBeginInteraction(final long gameTime, final UUID playerId) {
        if (gameTime == lastProcessedGameTime && playerId.equals(lastProcessedPlayer)) {
            return false;
        }
        lastProcessedGameTime = gameTime;
        lastProcessedPlayer = playerId;
        interactionCount++;
        setChanged();
        return true;
    }

    public OperationResult toggleAssembly() {
        final ControlledContraptionEntity active = getActiveContraption();
        if (active != null) {
            return disassembleActive();
        }
        return assembleFromWorld();
    }

    private OperationResult assembleFromWorld() {
        if (level == null || level.isClientSide) {
            return OperationResult.failure("server level unavailable");
        }
        if (!SimBlocks.PHYSICS_ASSEMBLER.has(getBlockState())) {
            return OperationResult.failure("assembly can only begin from a world Physics Assembler");
        }

        final BlockPos seed = worldPosition.relative(PhysicsAssemblerBlock.getStickyFacing(getBlockState()));
        final FabricAssemblyScanner.ScanResult scan = FabricAssemblyScanner.scan(level, worldPosition, seed);
        lastScanSummary = scan.stats().summary();
        if (!scan.successful()) {
            final BlockPos problem = scan.problemPos();
            final String suffix = problem == null ? "" : " at "
                    + problem.getX() + "," + problem.getY() + "," + problem.getZ();
            return fail(scan.error() + suffix);
        }
        if (scan.blocks().isEmpty()) {
            return fail("no movable structure was found");
        }

        final PhysicsAssemblyContraption contraption = new PhysicsAssemblyContraption(worldPosition);
        if (!contraption.assembleExact(level, scan)) {
            return fail("the exact Simulated scan could not be captured into Create transport data");
        }

        final int payloadCount = scan.blocks().size();
        final ControlledContraptionEntity entity = ControlledContraptionEntity.create(level, this, contraption);
        final Vec3 anchor = Vec3.atLowerCornerOf(worldPosition);
        entity.setPos(anchor.x, anchor.y, anchor.z);
        entity.setContraptionMotion(Vec3.ZERO);

        assemblyState = AssemblyState.ASSEMBLING;
        activeBlockCount = payloadCount;
        lastError = "";
        setChanged();

        // World mutation starts only after the full scanner set has been captured
        // and the Create entity has initialized every MovementContext.
        contraption.removeBlocksFromWorld(level, BlockPos.ZERO);

        if (!level.setBlock(worldPosition, SimBlocks.PHYSICS_ASSEMBLER_ANCHOR.get().defaultBlockState(), 3)) {
            entity.disassemble();
            restoreAssemblerIfMissing(level, worldPosition, contraption);
            return OperationResult.failure("could not create the temporary controller anchor; structure was restored");
        }

        final BlockEntity replacement = level.getBlockEntity(worldPosition);
        if (!(replacement instanceof final PhysicsAssemblerBlockEntity anchorController)) {
            level.removeBlock(worldPosition, false);
            entity.disassemble();
            restoreAssemblerIfMissing(level, worldPosition, contraption);
            return OperationResult.failure("controller anchor block entity was not created; structure was restored");
        }

        if (!level.addFreshEntity(entity)) {
            level.removeBlock(worldPosition, false);
            entity.disassemble();
            restoreAssemblerIfMissing(level, worldPosition, contraption);
            return OperationResult.failure("Create rejected the moving contraption entity; structure was restored");
        }

        anchorController.activeBlockCount = payloadCount;
        // The anchor is a fresh block entity, so carry the diagnostics across or
        // /simulated assembler_state reports nothing for a live assembly.
        anchorController.lastScanSummary = lastScanSummary;
        anchorController.interactionCount = interactionCount;
        anchorController.attach(entity);
        return OperationResult.success(
                "assembled exact Simulated scan; Physics Assembler is now part of the moving structure"
                        + " (Sable physics backend pending)",
                payloadCount);
    }

    public OperationResult disassembleActive() {
        return disassembleActive(true);
    }

    public OperationResult disassembleActive(final boolean relocateController) {
        if (level == null || level.isClientSide) {
            return OperationResult.failure("server level unavailable");
        }

        final ControlledContraptionEntity active = getActiveContraption();
        if (active == null) {
            clearActiveState();
            return OperationResult.failure("no live assembly is attached");
        }

        // Upstream gates disassembly on this assembler being the one that made the
        // sub-level. The check asks Sable which sub-level contains a position, and
        // Sable is the inert facade here, so it always permits -- but the call site
        // is kept so the rule arrives with V2 rather than having to be rediscovered.
        try {
            if (!DisassemblyPrevention.checkSubLevelForPrimary(level, getBlockPos())) {
                return OperationResult.failure("another Physics Assembler owns this assembly");
            }
        } catch (final AssemblyException exception) {
            return OperationResult.failure("disassembly refused: " + exception.getMessage());
        }

        if (SimBlocks.PHYSICS_ASSEMBLER_ANCHOR.has(getBlockState())) {
            return disassembleAnchoredTransport(active);
        }

        return disassembleLegacyTransport(active, relocateController);
    }

    /**
     * .10 does not trust Create to restore the assembler at local zero. Create
     * still owns payload placement, then Simulated verifies/restores the moving
     * assembler explicitly using the orientation persisted in the contraption.
     */
    private OperationResult disassembleAnchoredTransport(final ControlledContraptionEntity active) {
        final int placedBlocks = activeBlockCount;
        final BlockPos anchorPos = worldPosition.immutable();
        final PhysicsAssemblyContraption physicsAssembly = active.getContraption() instanceof PhysicsAssemblyContraption p
                ? p
                : null;
        final BlockPos assemblerTarget = physicsAssembly == null
                ? movedAssemblerTarget(active, anchorPos)
                : movedAssemblerTarget(active, physicsAssembly.getControllerPos() == null
                        ? anchorPos
                        : physicsAssembly.getControllerPos());

        active.setContraptionMotion(Vec3.ZERO);
        level.removeBlock(anchorPos, false);

        try {
            active.disassemble();

            if (physicsAssembly != null
                    && !restoreAssemblerIfMissing(level, assemblerTarget, physicsAssembly)) {
                return OperationResult.failure(
                        "payload disassembled, but Physics Assembler could not be restored at "
                                + assemblerTarget.getX() + "," + assemblerTarget.getY() + "," + assemblerTarget.getZ());
            }

            return OperationResult.success(
                    "disassembled moving assembler and payload back into world blocks",
                    placedBlocks);
        } catch (final RuntimeException exception) {
            // If Create fails before completing disassembly, recreate the anchor
            // so the still-live contraption remains controllable instead of
            // becoming an orphan that can corrupt the next world save.
            level.setBlock(anchorPos, SimBlocks.PHYSICS_ASSEMBLER_ANCHOR.get().defaultBlockState(), 3);
            if (level.getBlockEntity(anchorPos) instanceof final PhysicsAssemblerBlockEntity restoredAnchor
                    && active.isAlive()) {
                restoredAnchor.activeBlockCount = placedBlocks;
                restoredAnchor.attach(active);
                restoredAnchor.lastError = "disassembly failed: " + exception.getClass().getSimpleName();
                restoredAnchor.setChanged();
            }
            return OperationResult.failure("disassembly failed safely: " + exception.getClass().getSimpleName()
                    + (exception.getMessage() == null ? "" : " - " + exception.getMessage()));
        }
    }

    /** Compatibility path for live .6/.7 worlds where the assembler was left behind. */
    private OperationResult disassembleLegacyTransport(final ControlledContraptionEntity active,
                                                       final boolean relocateController) {
        final int placedBlocks = activeBlockCount;
        final BlockState controllerState = getBlockState();
        final Vec3 originalAnchor = Vec3.atLowerCornerOf(active.getContraption().anchor);
        final Vec3 translation = active.position().subtract(originalAnchor);
        final int dx = (int) Math.round(translation.x);
        final int dy = (int) Math.round(translation.y);
        final int dz = (int) Math.round(translation.z);
        final boolean translated = dx != 0 || dy != 0 || dz != 0;
        final BlockPos targetController = worldPosition.offset(dx, dy, dz);

        if (relocateController && translated && !level.isEmptyBlock(targetController)) {
            return OperationResult.failure(
                    "cannot disassemble legacy transport: translated assembler position is occupied at "
                            + targetController.getX() + "," + targetController.getY() + "," + targetController.getZ());
        }

        active.setContraptionMotion(Vec3.ZERO);
        active.disassemble();
        clearActiveState();

        if (relocateController && translated) {
            if (!level.setBlock(targetController, controllerState, 3)) {
                return OperationResult.failure(
                        "legacy structure was placed, but the assembler controller could not relocate");
            }
            level.removeBlock(worldPosition, false);
            return OperationResult.success(
                    "disassembled legacy transport; assembler moved " + dx + "," + dy + "," + dz,
                    placedBlocks);
        }

        return OperationResult.success("disassembled legacy transport back into world blocks", placedBlocks);
    }

    /**
     * Shutdown recovery for the temporary Create transport. Until Sable owns the
     * moving sub-level, live Physics Assembler transports are deliberately
     * returned to world blocks before an integrated/dedicated server stops.
     */
    public static boolean recoverTemporaryTransport(final ServerLevel world,
                                                    final ControlledContraptionEntity active) {
        if (!(active.getContraption() instanceof final PhysicsAssemblyContraption physicsAssembly)) {
            return false;
        }

        final BlockPos controllerPos = physicsAssembly.getControllerPos() == null
                ? active.getContraption().anchor
                : physicsAssembly.getControllerPos();
        final BlockPos assemblerTarget = movedAssemblerTarget(active, controllerPos);

        active.setContraptionMotion(Vec3.ZERO);
        if (SimBlocks.PHYSICS_ASSEMBLER_ANCHOR.has(world.getBlockState(controllerPos))) {
            world.removeBlock(controllerPos, false);
        }

        try {
            active.disassemble();
            return restoreAssemblerIfMissing(world, assemblerTarget, physicsAssembly);
        } catch (final RuntimeException exception) {
            return false;
        }
    }

    private static BlockPos movedAssemblerTarget(final ControlledContraptionEntity active,
                                                 final BlockPos controllerPos) {
        final Vec3 origin = Vec3.atLowerCornerOf(controllerPos);
        final Vec3 translation = active.position().subtract(origin);
        return controllerPos.offset(
                (int) Math.round(translation.x),
                (int) Math.round(translation.y),
                (int) Math.round(translation.z));
    }

    private static boolean restoreAssemblerIfMissing(final Level world,
                                                     final BlockPos target,
                                                     final PhysicsAssemblyContraption physicsAssembly) {
        if (SimBlocks.PHYSICS_ASSEMBLER.has(world.getBlockState(target))) {
            return true;
        }
        if (!world.isEmptyBlock(target)) {
            return false;
        }
        return world.setBlock(target, physicsAssembly.getAssemblerBlockState(), 3);
    }

    /**
     * Compatibility-only movement control until Sable supplies rigid-body
     * motion. Sneak-use moves the live assembly exactly one block in the player
     * requested cardinal direction, checking collision in quarter-block steps.
     */
    public OperationResult nudgeAssembly(final Direction direction) {
        final ControlledContraptionEntity active = getActiveContraption();
        if (active == null) {
            return OperationResult.failure("assemble a structure first");
        }

        final Vec3 step = Vec3.atLowerCornerOf(direction.getNormal()).scale(0.25D);

        for (int i = 0; i < 4; i++) {
            final Vec3 previous = active.position();
            active.setContraptionMotion(step);
            active.move(step.x, step.y, step.z);
            if (ContraptionCollider.collideBlocks(active)) {
                active.setPos(previous.x, previous.y, previous.z);
                active.setContraptionMotion(Vec3.ZERO);
                SimulatedFabricNetworking.syncContraptionPosition(active);
                return OperationResult.failure("movement blocked by world collision");
            }
        }

        active.setContraptionMotion(Vec3.ZERO);
        SimulatedFabricNetworking.syncContraptionPosition(active);
        return OperationResult.success("moved live assembly 1 block " + direction.getName(), activeBlockCount);
    }

    public boolean isHoldingAssembly() {
        return assemblyState == AssemblyState.ASSEMBLING || assemblyState == AssemblyState.ASSEMBLED;
    }

    public boolean hasActiveAssembly() {
        return getActiveContraption() != null;
    }

    public AssemblyState getAssemblyState() {
        return assemblyState;
    }

    public int getActiveBlockCount() {
        return activeBlockCount;
    }

    public int getInteractionCount() {
        return interactionCount;
    }

    public String getLastError() {
        return lastError;
    }

    public String getLastScanSummary() {
        return lastScanSummary;
    }

    private OperationResult fail(final String message) {
        assemblyState = AssemblyState.ERROR;
        lastError = message;
        activeBlockCount = 0;
        movedContraption = null;
        movedContraptionId = null;
        setChanged();
        return OperationResult.failure(message);
    }

    private ControlledContraptionEntity getActiveContraption() {
        if (movedContraption != null) {
            if (movedContraption.isAlive()) {
                return movedContraption;
            }
            movedContraption = null;
        }

        if (movedContraptionId != null && level instanceof final ServerLevel serverLevel) {
            if (serverLevel.getEntity(movedContraptionId) instanceof final ControlledContraptionEntity entity
                    && entity.isAlive()) {
                movedContraption = entity;
                return entity;
            }
        }
        return null;
    }

    private void clearActiveState() {
        movedContraption = null;
        movedContraptionId = null;
        activeBlockCount = 0;
        assemblyState = AssemblyState.IDLE;
        lastError = "";
        setChanged();
    }

    @Override
    public boolean isAttachedTo(final AbstractContraptionEntity contraption) {
        return movedContraption == contraption
                || movedContraptionId != null && movedContraptionId.equals(contraption.getUUID());
    }

    @Override
    public void attach(final ControlledContraptionEntity contraption) {
        movedContraption = contraption;
        movedContraptionId = contraption.getUUID();
        if (contraption.getContraption() instanceof final PhysicsAssemblyContraption physicsAssembly) {
            activeBlockCount = physicsAssembly.getPayloadBlockCount();
        } else if (contraption.getContraption() != null) {
            activeBlockCount = contraption.getContraption().getBlocks().size();
        }
        assemblyState = AssemblyState.ASSEMBLED;
        lastError = "";
        setChanged();
    }

    @Override
    public void onStall() {
        lastError = "Create transport contraption stalled on a collision";
        setChanged();
    }

    @Override
    public boolean isValid() {
        return !isRemoved();
    }

    @Override
    public BlockPos getBlockPosition() {
        return worldPosition;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("PortAssemblyState", assemblyState.name());
        tag.putInt("PortActiveBlockCount", activeBlockCount);
        tag.putInt("PortInteractionCount", interactionCount);
        tag.putString("PortLastError", lastError);
        if (movedContraptionId != null) {
            tag.putUUID("PortMovedContraption", movedContraptionId);
        }
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        try {
            assemblyState = AssemblyState.valueOf(tag.getString("PortAssemblyState"));
        } catch (final IllegalArgumentException ignored) {
            assemblyState = AssemblyState.IDLE;
        }
        activeBlockCount = tag.getInt("PortActiveBlockCount");
        interactionCount = tag.getInt("PortInteractionCount");
        lastError = tag.getString("PortLastError");
        movedContraptionId = tag.hasUUID("PortMovedContraption") ? tag.getUUID("PortMovedContraption") : null;
        movedContraption = null;

        if (movedContraptionId == null && assemblyState == AssemblyState.ASSEMBLED) {
            assemblyState = AssemblyState.IDLE;
            activeBlockCount = 0;
        }
    }

    /**
     * Lever animation, upstream's implementation unchanged. What differs here is
     * only which question decides the lever position: upstream asks whether the
     * block is inside a sub-level, this port asks whether its Create stand-in
     * contraption is assembled.
     */
    public void clientFlickLeverTo(final boolean flicked) {
        this.visualAngle.chase(flicked ? FLICKED_ANGLE_DEGREES : 0.0f, LEVER_CHASE_SPEED, LerpedFloat.Chaser.EXP);
    }

    public void jerkLever() {
        this.visualAngle.setValue(this.visualAngle.getChaseTarget());
        this.visualAngle.setValue(this.visualAngle.getChaseTarget());
    }

    protected void initializeLeverPosition() {
        if (!this.leverInitialized) {
            this.clientFlickLeverTo(this.isAssembled());
            this.jerkLever();
            this.leverInitialized = true;
        }
    }

    public void setClientHoldLeverInPlace(final boolean holding) {
        this.holdingLever = holding;
    }

    public void updateControlledByPlayer(final float angle) {
        this.controlledByPlayer = true;
        this.playerAngle = angle;
    }

    public boolean stopControllingPlayer() {
        if (!this.controlledByPlayer) {
            return false;
        }
        this.controlledByPlayer = false;
        return true;
    }

    public float getClientAngle(final float partialTicks) {
        return this.visualAngle.getValue(partialTicks);
    }

    /** Advances the lever animation; called from the block entity ticker. */
    public void tickLever() {
        if (this.holdingLever) {
            this.visualAngle.setValue(this.visualAngle.getValue());
        } else if (this.controlledByPlayer) {
            this.visualAngle.setValue(this.visualAngle.getValue());
            this.visualAngle.setValueNoUpdate(this.playerAngle);
        } else {
            this.visualAngle.tickChaser();
        }
    }

    /** Upstream's block entity is a Create SmartBlockEntity, which knows this. */
    public boolean isVirtual() {
        return false;
    }

    /**
     * Whether this assembler currently has a structure in the air. Upstream asks
     * whether the block sits inside a sub-level; the stand-in asks whether its
     * Create contraption exists.
     */
    /**
     * Whether this assembler is the one that made the structure it is part of.
     * Only one assembler may disassemble a structure; with the Create stand-in,
     * that is the one holding the contraption.
     */
    public boolean isPrimaryAssembler() {
        return this.isAssembled();
    }

    public boolean isAssembled() {
        return this.movedContraption != null && this.movedContraption.isAlive();
    }

}
