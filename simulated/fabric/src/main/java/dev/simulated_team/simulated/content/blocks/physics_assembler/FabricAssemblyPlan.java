package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Non-mutating preflight data for the future Sable assembly handoff.
 *
 * Upstream Simulated performs additional work immediately before moving blocks:
 * it carries Super Glue across, expands controlled Create contraptions and
 * transfers block entities with their blocks. This plan lets the Fabric port
 * validate those categories in a Homestead world before any world mutation is
 * enabled.
 */
public record FabricAssemblyPlan(int blockCount,
                                 int blockEntityCount,
                                 int fluidBlockCount,
                                 int superGlueSheetCount,
                                 int controlledContraptionCount,
                                 long glueSignature,
                                 AABB worldBounds) {

    public static FabricAssemblyPlan capture(final Level level,
                                             final FabricAssemblyScanner.ScanResult scan) {
        final Set<BlockPos> blocks = scan.blocks();
        int blockEntities = 0;
        int fluidBlocks = 0;

        for (final BlockPos pos : blocks) {
            if (level.getBlockEntity(pos) != null) {
                blockEntities++;
            }
            if (!level.getFluidState(pos).isEmpty()) {
                fluidBlocks++;
            }
        }

        final AABB bounds = new AABB(
                scan.min().getX(), scan.min().getY(), scan.min().getZ(),
                scan.max().getX() + 1.0, scan.max().getY() + 1.0, scan.max().getZ() + 1.0);

        final List<SuperGlueEntity> relevantGlues = new ArrayList<>();
        for (final SuperGlueEntity glue : level.getEntitiesOfClass(
                SuperGlueEntity.class, bounds.inflate(2.0))) {
            if (containsAny(glue, blocks)) {
                relevantGlues.add(glue);
            }
        }

        // Make the signature independent of entity iteration order.
        relevantGlues.sort(Comparator.comparing(FabricAssemblyPlan::boundsKey));
        long glueSignature = 0x6A09E667F3BCC909L;
        for (final SuperGlueEntity glue : relevantGlues) {
            final AABB box = glue.getBoundingBox();
            glueSignature = mix(glueSignature, Double.doubleToLongBits(box.minX));
            glueSignature = mix(glueSignature, Double.doubleToLongBits(box.minY));
            glueSignature = mix(glueSignature, Double.doubleToLongBits(box.minZ));
            glueSignature = mix(glueSignature, Double.doubleToLongBits(box.maxX));
            glueSignature = mix(glueSignature, Double.doubleToLongBits(box.maxY));
            glueSignature = mix(glueSignature, Double.doubleToLongBits(box.maxZ));
        }
        glueSignature = mix(glueSignature, relevantGlues.size());

        final int controlledContraptions = level.getEntitiesOfClass(
                ControlledContraptionEntity.class, bounds.inflate(2.0)).size();

        return new FabricAssemblyPlan(
                blocks.size(),
                blockEntities,
                fluidBlocks,
                relevantGlues.size(),
                controlledContraptions,
                glueSignature,
                bounds);
    }

    public boolean hasDeferredCreateContraptions() {
        return controlledContraptionCount > 0;
    }

    public String summary() {
        return blockCount + " blocks"
                + ", blockEntities=" + blockEntityCount
                + ", glueSheets=" + superGlueSheetCount
                + ", fluidBlocks=" + fluidBlockCount
                + ", createContraptions=" + controlledContraptionCount;
    }

    private static boolean containsAny(final SuperGlueEntity glue, final Set<BlockPos> blocks) {
        for (final BlockPos pos : blocks) {
            if (glue.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    private static String boundsKey(final SuperGlueEntity glue) {
        final AABB box = glue.getBoundingBox();
        return box.minX + ":" + box.minY + ":" + box.minZ + ":"
                + box.maxX + ":" + box.maxY + ":" + box.maxZ;
    }

    private static long mix(long hash, final long value) {
        hash ^= value + 0x9E3779B97F4A7C15L + (hash << 6) + (hash >>> 2);
        return hash;
    }
}
