package dev.simulated_team.simulated.backport.physics.sublevel.system;

import dev.simulated_team.simulated.backport.physics.api.physics.PhysicsPipeline;

/**
 * Stand-in for Sable's {@code SubLevelPhysicsSystem}. Nothing on this stack constructs
 * one — see the package documentation — so this exists to keep upstream's
 * declarations compiling until V2 brings the engine in.
 */
public class SubLevelPhysicsSystem {

    private net.minecraft.server.level.ServerLevel level;

    /** Re-reads a body's pose after something moved it. Nothing moves here. */
    public void updatePose(final dev.simulated_team.simulated.backport.physics.sublevel.SubLevel subLevel) {
    }

    /** The body behind a sub-level. There are none, so there is no handle. */
    @javax.annotation.Nullable
    public dev.simulated_team.simulated.backport.physics.api.physics.handle.RigidBodyHandle getPhysicsHandle(
            final dev.simulated_team.simulated.backport.physics.sublevel.SubLevel subLevel) {
        return null;
    }

    /** The system attached to a level. There is none, so there is nothing to get. */
    @javax.annotation.Nullable
    public static SubLevelPhysicsSystem get(final net.minecraft.world.level.Level level) {
        return null;
    }

    /** Whether the solver is paused. Nothing runs, so it is always paused. */
    public boolean getPaused() {
        return true;
    }

    public dev.simulated_team.simulated.backport.physics.sublevel.system.ticket.PhysicsChunkTicketManager
            getTicketManager() {
        return new dev.simulated_team.simulated.backport.physics.sublevel.system.ticket.PhysicsChunkTicketManager();
    }

    /** The tick a client interpolates rope and body poses against. */
    public int getInterpolationTick() {
        return 0;
    }

    /** Hands an object to the solver. There is no solver, so it is dropped. */
    public void addObject(@javax.annotation.Nullable final Object object) {
    }

    public void removeObject(@javax.annotation.Nullable final Object object) {
    }

    /** Solver settings. Vanilla-ish defaults; nothing reads them but the diagram. */
    public Config getConfig() {
        return new Config();
    }

    /** See {@link #getConfig()}. */
    public static class Config {
        public final int substepsPerTick = 4;
    }

    public PhysicsPipeline getPipeline() {
        return new PhysicsPipeline();
    }

    /** The level this system steps. Never set here: nothing steps. */
    public net.minecraft.server.level.ServerLevel getLevel() {
        return this.level;
    }

    public double getPartialPhysicsTick() {
        return 0.0;
    }

    /** Whether a chunk is loaded enough to simulate in. Nothing simulates here. */
    public static boolean isChunkLoadedEnough(final net.minecraft.server.level.ServerLevel level,
                                              final int chunkX, final int chunkZ) {
        return false;
    }
}
