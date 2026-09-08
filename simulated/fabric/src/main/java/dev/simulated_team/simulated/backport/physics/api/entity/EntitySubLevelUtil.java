package dev.simulated_team.simulated.backport.physics.api.entity;

import dev.simulated_team.simulated.backport.physics.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;

/**
 * Stand-in for Sable's {@code EntitySubLevelUtil}. Nothing on this stack constructs
 * one — see the package documentation — so this exists to keep upstream's
 * declarations compiling until V2 brings the engine in.
 */
public final class EntitySubLevelUtil {

    private EntitySubLevelUtil() {
    }

    @Nullable
    public static SubLevel getSubLevel(final Entity entity) {
        return null;
    }

    /** Throws an entity clear of a body. Nothing to be thrown clear of here. */
    public static void kickEntity(
            @Nullable final SubLevel subLevel, final Entity entity) {
    }

    /** With no sub-level under it, an entity's world position is its position. */
    public static Vec3 getWorldPosition(final Entity entity) {
        return entity.position();
    }
}
