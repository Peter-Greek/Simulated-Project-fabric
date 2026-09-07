package dev.simulated_team.simulated.backport.physics;

import dev.simulated_team.simulated.backport.physics.api.SubLevelHelper;

/**
 * Sable's entry point. Simulated reaches for exactly one member of it,
 * {@link #HELPER}, 149 times.
 */
public final class Sable {

    public static final String MOD_ID = "sable";

    public static final SubLevelHelper HELPER = new SubLevelHelper();

    private Sable() {
    }
}
