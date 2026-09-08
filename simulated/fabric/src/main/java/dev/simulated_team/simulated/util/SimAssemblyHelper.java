package dev.simulated_team.simulated.util;

import com.simibubi.create.content.contraptions.AssemblyException;
import dev.simulated_team.simulated.backport.physics.sublevel.SubLevel;
import dev.simulated_team.simulated.util.assembly.SimAssemblyException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.Nullable;

/**
 * Turning world blocks into a sub-level, and back.
 *
 * <p>This is the physics port itself, so on this stack it declines: an assembly
 * request throws the same {@link AssemblyException} a player would see for any
 * other refusal, and a disassembly request has nothing to disassemble. Callers
 * are unchanged and already handle both.
 *
 * <p>Upstream's version also moves entities, chunk tickets and plot bounds
 * across; all of that lands in V2 with Sable. The one piece kept here is
 * {@link #rotationFrom90DegRots}, which is arithmetic, not physics.
 */
public class SimAssemblyHelper {

    /** A completed assembly: the body that was made, and where it sits. */
    public record AssemblyResult(SubLevel subLevel, BlockPos offset) {
    }

    @Nullable
    public static AssemblyResult assembleFromSingleBlock(final Level level, final BlockPos selfPos,
                                                         final BlockPos toAssemble, final boolean includeStart,
                                                         final boolean includeEncasingGlue)
            throws AssemblyException {
        throw SimAssemblyException.noPhysicsEngine();
    }

    public static void disassembleSubLevel(final Level level, final SubLevel subLevel, final BlockPos from,
                                           final BlockPos to, final Rotation rotation, final boolean force) {
    }

    public static Rotation rotationFrom90DegRots(final int rots) {
        return switch (Math.floorMod(rots, 4)) {
            case 0 -> Rotation.NONE;
            case 1 -> Rotation.COUNTERCLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.CLOCKWISE_90;
            default -> throw new AssertionError(); // unreachable
        };
    }
}
