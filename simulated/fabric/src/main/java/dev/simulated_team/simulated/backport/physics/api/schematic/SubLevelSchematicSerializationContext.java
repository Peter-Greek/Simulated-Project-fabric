package dev.simulated_team.simulated.backport.physics.api.schematic;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * The context a sub-level is being saved into, or placed out of, a schematic.
 *
 * <p><b>Inert.</b> Nothing saves or places a sub-level here, so
 * {@link #getCurrentContext()} is always {@code null} and upstream's block
 * entities take their "no schematic in progress" path — which is what happens
 * during ordinary play anyway.
 */
public class SubLevelSchematicSerializationContext {

    /** Whether a schematic is being written or read. */
    public enum Type {
        SAVE,
        PLACE
    }

    /** How one sub-level in a schematic maps onto the one placed in the world. */
    public record SchematicMapping(UUID newUUID, java.util.function.UnaryOperator<BlockPos> transform) {
    }

    @Nullable
    public static SubLevelSchematicSerializationContext getCurrentContext() {
        return null;
    }

    public Type getType() {
        return Type.PLACE;
    }

    public UnaryOperator<BlockPos> getSetupTransform() {
        return UnaryOperator.identity();
    }

    public UnaryOperator<BlockPos> getPlaceTransform() {
        return UnaryOperator.identity();
    }

    @Nullable
    public SchematicMapping getMapping(final UUID id) {
        return null;
    }

    public net.minecraft.world.level.levelgen.structure.BoundingBox getBoundingBox() {
        return net.minecraft.world.level.levelgen.structure.BoundingBox.infinite();
    }
}
