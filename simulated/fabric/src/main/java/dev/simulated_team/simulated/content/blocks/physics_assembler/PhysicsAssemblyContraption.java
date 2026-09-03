package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.TranslatingContraption;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import dev.simulated_team.simulated.fabric.SimulatedFabricContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Transitional 1.20.1 transport backend for Physics Assembler structures.
 *
 * Sable remains the final physics backend. Until its 1.20.1 backport is ready,
 * this uses Create's contraption renderer/storage/collision implementation but
 * captures the exact block set discovered by the Simulated-parity scanner.
 */
public final class PhysicsAssemblyContraption extends TranslatingContraption {
    private BlockPos controllerPos;
    private int payloadBlockCount;
    private final List<SuperGlueEntity> pendingGlues = new ArrayList<>();

    public PhysicsAssemblyContraption() {
    }

    public PhysicsAssemblyContraption(final BlockPos controllerPos) {
        this.controllerPos = controllerPos.immutable();
    }

    /**
     * Build a Create transport from the exact block set already validated by
     * FabricAssemblyScanner. The Physics Assembler itself is captured at local
     * position zero so it visibly moves with the structure, matching upstream
     * Simulated's Sable sub-level behavior.
     */
    public boolean assembleExact(final Level world,
                                 final FabricAssemblyScanner.ScanResult scan) {
        if (controllerPos == null || !scan.successful()) {
            return false;
        }

        anchor = controllerPos;
        bounds = new AABB(BlockPos.ZERO);
        payloadBlockCount = scan.blocks().size();

        // Local zero is the moving Physics Assembler. The invisible controller
        // anchor that replaces it in the parent world is not part of this map.
        addBlock(world, controllerPos, capture(world, controllerPos));

        for (final BlockPos pos : scan.blocks()) {
            if (pos.equals(controllerPos)) {
                continue;
            }
            addBlock(world, pos, capture(world, pos));
        }

        pendingGlues.clear();
        pendingGlues.addAll(scan.glues());
        return payloadBlockCount > 0 && getBlocks().size() == payloadBlockCount + 1;
    }

    @Override
    public boolean assemble(final Level world, final BlockPos seed) throws AssemblyException {
        if (!searchMovedStructure(world, seed, null)) {
            return false;
        }
        payloadBlockCount = getBlocks().size();
        return !getBlocks().isEmpty();
    }

    /**
     * The scanner owns glue discovery, while Create owns glue serialization on
     * the moving contraption. Convert the scanner's world-space glue boxes to
     * Create's local-space representation immediately before world mutation.
     */
    @Override
    public void removeBlocksFromWorld(final Level world, final BlockPos offset) {
        if (!pendingGlues.isEmpty()) {
            final Vec3 localOffset = Vec3.atLowerCornerOf(offset.offset(anchor)).scale(-1.0D);
            for (final SuperGlueEntity glue : pendingGlues) {
                if (!glue.isRemoved()) {
                    superglue.add(glue.getBoundingBox().move(localOffset));
                    glue.discard();
                }
            }
            pendingGlues.clear();
        }
        super.removeBlocksFromWorld(world, offset);
    }

    /**
     * Create captures movement-capable blocks with a null MovementContext and
     * expects startMoving() to populate those contexts before the contraption
     * is serialized for its spawn packet or a world save.
     */
    @Override
    public void onEntityCreated(final AbstractContraptionEntity entity) {
        super.onEntityCreated(entity);
        startMoving(entity.level());
    }

    /**
     * There is deliberately no anchoring block inside this moving contraption.
     *
     * Create normally excludes its controller/anchor from both capture and
     * restoration. In this port local position zero is the real Physics
     * Assembler and must be removed, rendered, moved and restored like every
     * other captured block. The stable Create controller is a separate invisible
     * PHYSICS_ASSEMBLER_ANCHOR block in the parent world and is never present in
     * this contraption's block map.
     */
    @Override
    protected boolean isAnchoringBlockAt(final BlockPos pos) {
        return false;
    }

    @Override
    public boolean canBeStabilized(final net.minecraft.core.Direction facing, final BlockPos localPos) {
        return true;
    }

    @Override
    public ContraptionType getType() {
        return SimulatedFabricContent.physicsAssemblyContraptionType();
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public int getPayloadBlockCount() {
        return payloadBlockCount;
    }

    @Override
    public CompoundTag writeNBT(final boolean spawnPacket) {
        final CompoundTag tag = super.writeNBT(spawnPacket);
        if (controllerPos != null) {
            tag.putLong("SimulatedController", controllerPos.asLong());
        }
        tag.putInt("SimulatedPayloadBlockCount", payloadBlockCount);
        return tag;
    }

    @Override
    public void readNBT(final Level world, final CompoundTag nbt, final boolean spawnData) {
        controllerPos = nbt.contains("SimulatedController")
                ? BlockPos.of(nbt.getLong("SimulatedController"))
                : null;
        payloadBlockCount = nbt.getInt("SimulatedPayloadBlockCount");
        super.readNBT(world, nbt, spawnData);
        if (payloadBlockCount <= 0 && !getBlocks().isEmpty()) {
            // Old .6/.7 worlds did not store a separate payload count and did
            // not carry the assembler inside the contraption.
            payloadBlockCount = getBlocks().size();
        }
    }
}
