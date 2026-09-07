package dev.simulated_team.simulated.backport.physics.api.sublevel;

import dev.simulated_team.simulated.backport.physics.sublevel.SubLevel;
import dev.simulated_team.simulated.backport.physics.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * A level's collection of sub-levels. Every level here holds none, so lookups
 * come back empty and {@link #getContainer} hands out a shared empty container
 * rather than {@code null} — upstream call sites chain straight off it.
 */
public class SubLevelContainer {

    private static final SubLevelContainer EMPTY = new SubLevelContainer();

    private final SubLevelPhysicsSystem physicsSystem = new SubLevelPhysicsSystem();

    protected SubLevelContainer() {
    }

    public static SubLevelContainer getContainer(final Level level) {
        return EMPTY;
    }

    /**
     * Upstream calls this with a cast to {@code ServerLevel} and assigns the
     * result to a {@code ServerSubLevelContainer}, so the overload has to be
     * here rather than only on the subclass.
     */
    public static ServerSubLevelContainer getContainer(final net.minecraft.server.level.ServerLevel level) {
        return ServerSubLevelContainer.getContainer(level);
    }


    @Nullable
    public SubLevel getSubLevel(final UUID id) {
        return null;
    }

    @Nullable
    public SubLevel getSubLevel(final int id) {
        return null;
    }

    public Collection<SubLevel> getSubLevels() {
        return Collections.emptyList();
    }

    /** Upstream's name for the same collection. */
    public Collection<SubLevel> subLevels() {
        return Collections.emptyList();
    }

    /** Every sub-level in this container. None, here. */
    public Collection<SubLevel> getAllSubLevels() {
        return Collections.emptyList();
    }

    /** Whether a chunk belongs to any body here. None does. */
    public boolean inBounds(final net.minecraft.world.level.ChunkPos chunk) {
        return false;
    }

    public SubLevelPhysicsSystem physicsSystem() {
        return this.physicsSystem;
    }
}
