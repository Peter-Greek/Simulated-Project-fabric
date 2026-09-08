package dev.simulated_team.simulated.backport.physics.sublevel.system;

/** Which players are watching which sub-levels. Empty here. */
public class SubLevelTrackingSystem {

    /** Accepted and never consulted: there is nothing to track. */
    public void addTrackingPlugin(final Object plugin) {
    }

    /** The tick clients interpolate tracked poses against. */
    public int getInterpolationTick() {
        return 0;
    }
}
