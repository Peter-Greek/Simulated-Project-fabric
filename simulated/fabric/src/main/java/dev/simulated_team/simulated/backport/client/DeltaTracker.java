package dev.simulated_team.simulated.backport.client;

/**
 * Stand-in for {@code net.minecraft.client.DeltaTracker} (1.21).
 *
 * <p>1.21 replaced the bare {@code float partialTick} parameter with an object
 * that can also report the real-time delta and whether the game is paused.
 * Simulated only ever asks it for the partial tick, so this wraps that one
 * float and keeps upstream's call sites unchanged.
 */
public class DeltaTracker {

    private final float partialTick;

    public DeltaTracker(final float partialTick) {
        this.partialTick = partialTick;
    }

    public float getGameTimeDeltaPartialTick(final boolean runsNormally) {
        return this.partialTick;
    }

    public float getGameTimeDeltaTicks() {
        return this.partialTick;
    }

    public float getRealtimeDeltaTicks() {
        return this.partialTick;
    }
}
