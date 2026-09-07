package dev.simulated_team.simulated.backport.physics.api.sublevel;

import dev.simulated_team.simulated.backport.physics.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.UUID;

/** Server half of {@link SubLevelContainer}; holds no sub-levels here. */
public class ServerSubLevelContainer extends SubLevelContainer {

    private static final ServerSubLevelContainer EMPTY = new ServerSubLevelContainer();

    protected ServerSubLevelContainer() {
    }

    public static ServerSubLevelContainer getContainer(final ServerLevel level) {
        return EMPTY;
    }

    @Nullable
    @Override
    public ServerSubLevel getSubLevel(final UUID id) {
        return null;
    }

    @Nullable
    public ServerSubLevel allocateNewSubLevel() {
        return null;
    }

    @Nullable
    public ServerSubLevel allocateNewSubLevel(
            final dev.simulated_team.simulated.backport.physics.companion.math.Pose3dc pose) {
        return null;
    }

    /** Never set: this container is never attached to a level. */
    public ServerLevel getLevel() {
        return null;
    }

    /** Accepted and never called back: no sub-level is ever created or removed. */
    public void addObserver(final Object observer) {
    }

    public dev.simulated_team.simulated.backport.physics.sublevel.system.SubLevelTrackingSystem trackingSystem() {
        return new dev.simulated_team.simulated.backport.physics.sublevel.system.SubLevelTrackingSystem();
    }
}
