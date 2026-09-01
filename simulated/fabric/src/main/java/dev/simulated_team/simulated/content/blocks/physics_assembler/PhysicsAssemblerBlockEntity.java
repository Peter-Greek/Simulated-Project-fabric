package dev.simulated_team.simulated.content.blocks.physics_assembler;

import dev.simulated_team.simulated.fabric.SimulatedFabricContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Minimal 1.20.1 block entity used to prove registration and persistence
 * before Sable assembly state is backported.
 */
public final class PhysicsAssemblerBlockEntity extends BlockEntity {
    private int interactionCount;

    public PhysicsAssemblerBlockEntity(final BlockPos pos, final BlockState state) {
        super(SimulatedFabricContent.PHYSICS_ASSEMBLER_BLOCK_ENTITY, pos, state);
    }

    public int recordInteraction() {
        interactionCount++;
        setChanged();
        return interactionCount;
    }

    public int getInteractionCount() {
        return interactionCount;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("PortInteractionCount", interactionCount);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        interactionCount = tag.getInt("PortInteractionCount");
    }
}
