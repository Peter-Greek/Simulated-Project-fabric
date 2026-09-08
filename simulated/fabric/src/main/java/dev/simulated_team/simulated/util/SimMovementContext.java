package dev.simulated_team.simulated.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;

/**
 * Where a block sits, and how it is oriented, relative to the world.
 *
 * <p>Upstream resolves the containing Sable sub-level and reports the block's
 * global pose through it. There are no sub-levels on this stack until V2, so
 * every position resolves to the identity context: local equals global, and the
 * orientation is unrotated. Call sites are unchanged and pick up real poses when
 * Sable lands; {@code subLevel} is an accessor rather than a record component,
 * because a record component of that type would have to be constructed and
 * there is nothing to construct it from.
 */
public record SimMovementContext(Level level, Vec3 localPosition, Vec3 globalPosition, Quaterniond orientation) {

    public static SimMovementContext getMovementContext(final Level level, final Vec3 position) {
        return new SimMovementContext(level, position, position, new Quaterniond());
    }

    /**
     * The body this position sits on, or null when it is in the world. Always
     * null here — see the class documentation — so callers compare equal only
     * when both sides are in the world, which is the correct answer.
     */
    @javax.annotation.Nullable
    public dev.simulated_team.simulated.backport.physics.sublevel.SubLevel subLevel() {
        return null;
    }

    public BlockPos localBlockPos() {
        return BlockPos.containing(this.localPosition.x(), this.localPosition.y(), this.localPosition.z());
    }
}
