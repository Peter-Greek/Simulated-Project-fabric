package dev.simulated_team.simulated.content.blocks.physics_assembler;

import dev.simulated_team.simulated.fabric.SimulatedFabricContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.20.1 Physics Assembler block entity.
 *
 * Until Sable itself is backported, this owns the complete pre-physics assembly
 * lifecycle: discovery, preparation, repeat validation, invalidation and
 * persistence. Only compact metadata is saved; block positions are rescanned
 * from the world before any future physics handoff.
 */
public final class PhysicsAssemblerBlockEntity extends BlockEntity {
    public enum AssemblyState {
        IDLE,
        PREPARED,
        VALIDATED,
        ERROR
    }

    public enum PreparationResult {
        PREPARED,
        UPDATED,
        VALIDATED
    }

    private int interactionCount;
    private int lastScannedBlockCount;
    private boolean lastScanSuccessful;
    private int stableValidationCount;
    private AssemblyState assemblyState = AssemblyState.IDLE;
    private PreparedAssembly preparedAssembly;
    private String lastError = "";

    public PhysicsAssemblerBlockEntity(final BlockPos pos, final BlockState state) {
        super(SimulatedFabricContent.PHYSICS_ASSEMBLER_BLOCK_ENTITY, pos, state);
    }

    public int recordInteraction() {
        interactionCount++;
        setChanged();
        return interactionCount;
    }

    public PreparationResult prepare(final PreparedAssembly snapshot) {
        final boolean hadPreparedAssembly = preparedAssembly != null;
        final boolean unchanged = snapshot.equals(preparedAssembly);

        preparedAssembly = snapshot;
        lastScannedBlockCount = snapshot.blockCount();
        lastScanSuccessful = true;
        lastError = "";

        if (unchanged) {
            stableValidationCount++;
            assemblyState = AssemblyState.VALIDATED;
            setChanged();
            return PreparationResult.VALIDATED;
        }

        stableValidationCount = 0;
        assemblyState = AssemblyState.PREPARED;
        setChanged();
        return hadPreparedAssembly ? PreparationResult.UPDATED : PreparationResult.PREPARED;
    }

    public void recordFailedScan(final String error) {
        lastScannedBlockCount = 0;
        lastScanSuccessful = false;
        stableValidationCount = 0;
        assemblyState = AssemblyState.ERROR;
        preparedAssembly = null;
        lastError = error == null ? "Unknown scan error" : error;
        setChanged();
    }

    public boolean clearPreparedAssembly() {
        final boolean changed = preparedAssembly != null || assemblyState != AssemblyState.IDLE || !lastError.isEmpty();
        preparedAssembly = null;
        lastScannedBlockCount = 0;
        lastScanSuccessful = false;
        stableValidationCount = 0;
        assemblyState = AssemblyState.IDLE;
        lastError = "";
        if (changed) {
            setChanged();
        }
        return changed;
    }

    public int getInteractionCount() {
        return interactionCount;
    }

    public int getLastScannedBlockCount() {
        return lastScannedBlockCount;
    }

    public boolean wasLastScanSuccessful() {
        return lastScanSuccessful;
    }

    public int getStableValidationCount() {
        return stableValidationCount;
    }

    public AssemblyState getAssemblyState() {
        return assemblyState;
    }

    public PreparedAssembly getPreparedAssembly() {
        return preparedAssembly;
    }

    public String getLastError() {
        return lastError;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("PortInteractionCount", interactionCount);
        tag.putInt("PortLastScannedBlockCount", lastScannedBlockCount);
        tag.putBoolean("PortLastScanSuccessful", lastScanSuccessful);
        tag.putInt("PortStableValidationCount", stableValidationCount);
        tag.putString("PortAssemblyState", assemblyState.name());
        tag.putString("PortLastError", lastError);
        if (preparedAssembly != null) {
            tag.put("PortPreparedAssembly", preparedAssembly.write());
        }
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        interactionCount = tag.getInt("PortInteractionCount");
        lastScannedBlockCount = tag.getInt("PortLastScannedBlockCount");
        lastScanSuccessful = tag.getBoolean("PortLastScanSuccessful");
        stableValidationCount = tag.getInt("PortStableValidationCount");
        lastError = tag.getString("PortLastError");

        try {
            assemblyState = AssemblyState.valueOf(tag.getString("PortAssemblyState"));
        } catch (final IllegalArgumentException ignored) {
            assemblyState = AssemblyState.IDLE;
        }

        preparedAssembly = tag.contains("PortPreparedAssembly")
                ? PreparedAssembly.read(tag.getCompound("PortPreparedAssembly"))
                : null;

        if (preparedAssembly == null && assemblyState != AssemblyState.ERROR) {
            assemblyState = AssemblyState.IDLE;
        }
    }
}
