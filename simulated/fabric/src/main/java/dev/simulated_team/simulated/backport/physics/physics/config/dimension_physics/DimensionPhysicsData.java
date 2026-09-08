package dev.simulated_team.simulated.backport.physics.physics.config.dimension_physics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3dc;


/**
 * Per-dimension gravity and air pressure.
 *
 * <p>Sable makes these data-driven per dimension. Without it, the values are
 * Minecraft's own: vanilla gravity, and a pressure of one atmosphere everywhere,
 * which is what the sensors read at ground level. V2 restores the data pack.
 */
public final class DimensionPhysicsData {

    /** Vanilla entity gravity, in blocks per tick squared, as a downward vector. */
    private static final Vector3dc VANILLA_GRAVITY = new Vector3d(0.0, -0.08, 0.0);

    private DimensionPhysicsData() {
    }

    public static Vector3d getGravity(final Level level) {
        return new Vector3d(VANILLA_GRAVITY);
    }

    public static Vector3d getGravity(final Level level, final BlockPos pos) {
        return new Vector3d(VANILLA_GRAVITY);
    }

    public static Vector3d getGravity(final Level level, final Vector3dc position) {
        return new Vector3d(VANILLA_GRAVITY);
    }

    public static double getAirPressure(final Level level, final Vector3dc position) {
        return 1.0;
    }

    public static double getAirPressure(final Level level, final BlockPos pos) {
        return 1.0;
    }
}
