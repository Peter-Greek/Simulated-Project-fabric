package dev.simulated_team.simulated.content.blocks.physics_assembler;

import dev.simulated_team.simulated.fabric.SimulatedFabricContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.20.1 Physics Assembler block entity.
 *
 * Persistence is intentionally kept small while the Sable sub-level state is
 * being backported. The scan fields prove the assembly-discovery stage survives
 * world reloads independently from physics state.
 */
public final class PhysicsAssemblerBlockEntity extends BlockEntity {
    private int interactionCount;
    private int lastScannedBlockCount;
    private boolean lastScanSuccessful;

    public PhysicsAssemblerBlockEntity(final BlockPos pos, final BlockState state) {
        super(SimulatedFabricContent.PHYSICS_ASSEMBLER_BLOCK_ENTITY, pos, state);
    }

    public int recordInteraction() {
        interactionCount++;
        setChanged();
        return interactionCount;
    }

    public void recordSuccessfulScan(final int blockCount) {
        lastScannedBlockCount = blockCount;
        lastScanSuccessful = true;
        setChanged();
    }

    public void recordFailedScan() {
        lastScannedBlockCount = 0;
        lastScanSuccessful = false;
        setChanged();
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

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("PortInteractionCount", interactionCount);
        tag.putInt("PortLastScannedBlockCount", lastScannedBlockCount);
        tag.putBoolean("PortLastScanSuccessful", lastScanSuccessful);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        interactionCount = tag.getInt("PortInteractionCount");
        lastScannedBlockCount = tag.getInt("PortLastScannedBlockCount");
        lastScanSuccessful = tag.getBoolean("PortLastScanSuccessful");
    }
}
