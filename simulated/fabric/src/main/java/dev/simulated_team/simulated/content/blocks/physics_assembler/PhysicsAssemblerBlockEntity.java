package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IControlContraption;
import dev.simulated_team.simulated.fabric.SimulatedFabricContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Fabric 1.20.1 Physics Assembler controller.
 *
 * This milestone performs real world mutation. Until Sable's sublevel/physics
 * backend is available on 1.20.1, a Create ControlledContraptionEntity is used
 * as the transport layer. The assembler remains a world-side controller while
 * the structure is moving, then follows the structure when it is disassembled.
 */
public final class PhysicsAssemblerBlockEntity extends BlockEntity implements IControlContraption {
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
    private int activeBlockCount;
    private int interactionCount;
    private String lastError = "";

    private long lastProcessedGameTime = Long.MIN_VALUE;
    private UUID lastProcessedPlayer;

    public PhysicsAssemblerBlockEntity(final BlockPos pos, final BlockState state) {
        super(SimulatedFabricContent.PHYSICS_ASSEMBLER_BLOCK_ENTITY, pos, state);
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

        final BlockPos seed = worldPosition.relative(PhysicsAssemblerBlock.getStickyFacing(getBlockState()));
        if (level.getBlockState(seed).isAir()) {
            return fail("nothing is attached to the assembler");
        }

        try {
            final PhysicsAssemblyContraption contraption = new PhysicsAssemblyContraption(worldPosition);
            if (!contraption.assemble(level, seed) || contraption.getBlocks().isEmpty()) {
                return fail("no movable structure was found");
            }

            final int blockCount = contraption.getBlocks().size();

            // Mark active before world removal. The support block immediately
            // next to the assembler is normally part of the moving structure;
            // this prevents support updates from deleting the fixed controller
            // while that block is being captured.
            assemblyState = AssemblyState.ASSEMBLING;
            activeBlockCount = blockCount;
            lastError = "";
            setChanged();

            contraption.removeBlocksFromWorld(level, BlockPos.ZERO);

            final ControlledContraptionEntity entity = ControlledContraptionEntity.create(level, this, contraption);
            final Vec3 anchor = Vec3.atLowerCornerOf(contraption.anchor);
            entity.setPos(anchor.x, anchor.y, anchor.z);
            entity.setContraptionMotion(Vec3.ZERO);

            if (!level.addFreshEntity(entity)) {
                entity.disassemble();
                return fail("Create rejected the moving contraption entity");
            }

            attach(entity);
            assemblyState = AssemblyState.ASSEMBLED;
            lastError = "";
            setChanged();
            return OperationResult.success(
                    "assembled into a live Create transport contraption (Sable physics backend pending)",
                    blockCount);
        } catch (final AssemblyException exception) {
            final String detail = exception.getMessage();
            return fail(detail == null || detail.isBlank() ? "Create rejected the structure" : detail);
        } catch (final RuntimeException exception) {
            return fail("assembly failed: " + exception.getClass().getSimpleName()
                    + (exception.getMessage() == null ? "" : " - " + exception.getMessage()));
        }
    }

    public OperationResult disassembleActive() {
        return disassembleActive(true);
    }

    /**
     * Places the moving structure back into the world. When the player is
     * intentionally toggling the assembler, the controller follows the exact
     * integral translation of the temporary Create transport. When the block is
     * being destroyed, relocation is disabled so vanilla can finish the break.
     */
    public OperationResult disassembleActive(final boolean relocateController) {
        if (level == null || level.isClientSide) {
            return OperationResult.failure("server level unavailable");
        }

        final ControlledContraptionEntity active = getActiveContraption();
        if (active == null) {
            clearActiveState();
            return OperationResult.failure("no live assembly is attached");
        }

        final int placedBlocks = activeBlockCount;
        final BlockState controllerState = getBlockState();
        final Vec3 originalAnchor = Vec3.atLowerCornerOf(active.getContraption().anchor);
        final Vec3 translation = active.position().subtract(originalAnchor);
        final int dx = (int) Math.round(translation.x);
        final int dy = (int) Math.round(translation.y);
        final int dz = (int) Math.round(translation.z);
        final boolean translated = dx != 0 || dy != 0 || dz != 0;
        final BlockPos targetController = worldPosition.offset(dx, dy, dz);

        // The controller was deliberately excluded from the captured structure,
        // so its translated destination should be empty until disassembly puts
        // the neighboring support block back. Refuse to overwrite real blocks.
        if (relocateController && translated && !level.isEmptyBlock(targetController)) {
            return OperationResult.failure(
                    "cannot disassemble: translated assembler position is occupied at "
                            + targetController.getX() + "," + targetController.getY() + "," + targetController.getZ());
        }

        active.setContraptionMotion(Vec3.ZERO);
        active.disassemble();
        clearActiveState();

        if (relocateController && translated) {
            if (!level.setBlock(targetController, controllerState, 3)) {
                return OperationResult.failure(
                        "structure was placed, but the assembler controller could not relocate");
            }

            // State was cleared before this removal, so onRemove will not try to
            // disassemble the already-placed contraption a second time.
            level.removeBlock(worldPosition, false);
            return OperationResult.success(
                    "disassembled back into world blocks; assembler moved "
                            + dx + "," + dy + "," + dz,
                    placedBlocks);
        }

        return OperationResult.success("disassembled back into world blocks", placedBlocks);
    }

    /**
     * Compatibility-only movement control for the first real transport build.
     * Sneak-use moves the live Create contraption one block horizontally with
     * collision checks. Sable will replace this with actual rigid-body motion.
     */
    public OperationResult nudgeAssembly(final Direction direction) {
        final ControlledContraptionEntity active = getActiveContraption();
        if (active == null) {
            return OperationResult.failure("assemble a structure first");
        }

        final Direction horizontal = direction.getAxis().isHorizontal() ? direction : Direction.NORTH;
        final Vec3 step = Vec3.atLowerCornerOf(horizontal.getNormal()).scale(0.25D);

        for (int i = 0; i < 4; i++) {
            final Vec3 previous = active.position();
            active.setContraptionMotion(step);
            active.move(step.x, step.y, step.z);
            if (ContraptionCollider.collideBlocks(active)) {
                active.setPos(previous.x, previous.y, previous.z);
                active.setContraptionMotion(Vec3.ZERO);
                return OperationResult.failure("movement blocked by world collision");
            }
        }

        active.setContraptionMotion(Vec3.ZERO);
        return OperationResult.success("moved live assembly 1 block " + horizontal.getName(), activeBlockCount);
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
        activeBlockCount = contraption.getContraption() == null
                ? activeBlockCount
                : contraption.getContraption().getBlocks().size();
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

        // Old .5 PREPARED/VALIDATED data is intentionally not trusted as a
        // live assembly. Only an entity UUID represents real assembled state.
        if (movedContraptionId == null && assemblyState == AssemblyState.ASSEMBLED) {
            assemblyState = AssemblyState.IDLE;
            activeBlockCount = 0;
        }
    }
}
