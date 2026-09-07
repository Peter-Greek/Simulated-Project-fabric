package dev.simulated_team.simulated.backport.physics.sublevel;

import dev.simulated_team.simulated.backport.physics.api.physics.force.ForceGroup;
import dev.simulated_team.simulated.backport.physics.api.physics.force.QueuedForceGroup;
import dev.simulated_team.simulated.backport.physics.api.physics.mass.MassTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Stand-in for Sable's {@code ServerSubLevel}. Nothing on this stack constructs
 * one — see the package documentation — so this exists to keep upstream's
 * declarations compiling until V2 brings the engine in.
 */
public class ServerSubLevel extends SubLevel {

    protected ServerSubLevel(final ServerLevel level) {
        super(level);
    }

    public MassTracker getMassTracker() {
        return new MassTracker();
    }

    /** Forces are accepted and discarded: there is nothing to push on. */
    public QueuedForceGroup getOrCreateQueuedForceGroup(final ForceGroup group) {
        return new QueuedForceGroup();
    }

    public it.unimi.dsi.fastutil.objects.Object2ObjectMap<ForceGroup, QueuedForceGroup> getQueuedForceGroups() {
        return new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>();
    }

    public void enableIndividualQueuedForcesTracking(final boolean enabled) {
    }

    public void enableIndividualQueuedForcesTracking() {
    }

    /** Upstream iterates these by id. Nobody tracks a sub-level here. */
    public Collection<java.util.UUID> getTrackingPlayers() {
        return Collections.emptyList();
    }
}
