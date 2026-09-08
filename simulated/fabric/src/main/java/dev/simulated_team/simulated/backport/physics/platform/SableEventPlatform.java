package dev.simulated_team.simulated.backport.physics.platform;

/**
 * Sable's loader-neutral event bus. Nothing fires here, so listeners registered
 * through it simply never run.
 */
public final class SableEventPlatform {

    public static final SableEventPlatform INSTANCE = new SableEventPlatform();

    private SableEventPlatform() {
    }

    /**
     * Fired when a level's sub-level container is ready. No container is ever
     * created here, so a listener is recorded and never called.
     */
    public void onSubLevelContainerReady(
            final java.util.function.BiConsumer<net.minecraft.world.level.Level,
                    dev.simulated_team.simulated.backport.physics.api.sublevel.SubLevelContainer> listener) {
    }

    public void onSubLevelTick(final Runnable listener) {
    }

    /**
     * Fired once per physics substep, and once after the last one. Nothing steps
     * here, so a listener is recorded and never called — which is what leaves
     * every force, magnet and buoyancy pass idle until V2.
     */
    public void onPhysicsTick(final PhysicsTickListener listener) {
    }

    public void onPostPhysicsTick(final PhysicsTickListener listener) {
    }

    /** See {@link #onPhysicsTick}. */
    @FunctionalInterface
    public interface PhysicsTickListener {
        void tick(dev.simulated_team.simulated.backport.physics.sublevel.system.SubLevelPhysicsSystem system,
                  double timeStep);
    }
}
