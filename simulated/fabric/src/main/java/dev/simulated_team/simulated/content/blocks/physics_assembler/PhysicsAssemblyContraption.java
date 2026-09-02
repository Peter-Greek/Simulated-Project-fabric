package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.TranslatingContraption;
import dev.simulated_team.simulated.fabric.SimulatedFabricContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * Transitional 1.20.1 transport backend for Physics Assembler structures.
 *
 * Sable remains the final physics backend. Until its 1.20.1 backport is ready,
 * this delegates world capture, block-entity transfer, Super Glue handling and
 * moving-block rendering to Create's native contraption implementation. That
 * makes assembly/disassembly real instead of a dry-run while preserving the
 * same controller/seed relationship used by Simulated.
 */
public final class PhysicsAssemblyContraption extends TranslatingContraption {
    private BlockPos controllerPos;

    public PhysicsAssemblyContraption() {
    }

    public PhysicsAssemblyContraption(final BlockPos controllerPos) {
        this.controllerPos = controllerPos.immutable();
    }

    @Override
    public boolean assemble(final Level world, final BlockPos seed) throws AssemblyException {
        if (!searchMovedStructure(world, seed, null)) {
            return false;
        }
        return !getBlocks().isEmpty();
    }

    /**
     * Create normally treats its search anchor as immovable. The Physics
     * Assembler's controller is the actual anchor and the adjacent seed block
     * must be allowed into the moving structure.
     */
    @Override
    protected boolean isAnchoringBlockAt(final BlockPos pos) {
        return controllerPos != null && controllerPos.equals(pos);
    }

    @Override
    public boolean canBeStabilized(final net.minecraft.core.Direction facing, final BlockPos localPos) {
        return true;
    }

    @Override
    public ContraptionType getType() {
        return SimulatedFabricContent.physicsAssemblyContraptionType();
    }

    @Override
    public CompoundTag writeNBT(final boolean spawnPacket) {
        final CompoundTag tag = super.writeNBT(spawnPacket);
        if (controllerPos != null) {
            tag.putLong("SimulatedController", controllerPos.asLong());
        }
        return tag;
    }

    @Override
    public void readNBT(final Level world, final CompoundTag nbt, final boolean spawnData) {
        controllerPos = nbt.contains("SimulatedController")
                ? BlockPos.of(nbt.getLong("SimulatedController"))
                : null;
        super.readNBT(world, nbt, spawnData);
    }
}
