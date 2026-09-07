package dev.simulated_team.simulated.backport.physics.api;

import dev.simulated_team.simulated.backport.physics.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.Collection;


/**
 * Turns a set of world blocks into a sub-level and back. This is the heart of
 * V2; here it declines every request, which is what leaves the Physics Assembler
 * on its Create stand-in.
 */
public class SubLevelAssemblyHelper {

    @Nullable
    public ServerSubLevel assemble(final ServerLevel level, final Collection<BlockPos> blocks) {
        return null;
    }

    public boolean disassemble(final ServerSubLevel subLevel) {
        return false;
    }

    /** Moves a body out of the one containing it. Nothing contains anything here. */
    public static void kickFromContainingSubLevel(
            final ServerLevel level,
            final dev.simulated_team.simulated.backport.physics.sublevel.system.SubLevelPhysicsSystem system,
            final dev.simulated_team.simulated.backport.physics.api.physics.PhysicsPipeline pipeline,
            final ServerSubLevel body,
            final dev.simulated_team.simulated.backport.physics.sublevel.SubLevel container) {
    }
}
