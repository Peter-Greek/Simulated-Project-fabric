package dev.simulated_team.simulated.backport.physics.api.sublevel;

/** Extra per-player tracking alongside a sub-level. Never consulted. */
public interface SubLevelTrackingPlugin {

    /** Which players need this plugin's data this tick. Nothing is tracked here. */
    default Iterable<java.util.UUID> neededPlayers() {
        return java.util.List.of();
    }

    default void sendTrackingData(final int interpolationTick) {
    }
}
