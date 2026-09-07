package dev.simulated_team.simulated.backport.physics.util;

import io.netty.buffer.ByteBuf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Stand-in for Sable's {@code SableBufferUtils}. Nothing on this stack constructs
 * one — see the package documentation — so this exists to keep upstream's
 * declarations compiling until V2 brings the engine in.
 */
public final class SableBufferUtils {

    private SableBufferUtils() {
    }

    public static void write(final ByteBuf buffer, final Vector3dc vector) {
        buffer.writeDouble(vector.x());
        buffer.writeDouble(vector.y());
        buffer.writeDouble(vector.z());
    }

    public static Vector3d read(final ByteBuf buffer, final Vector3d destination) {
        return destination.set(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void write(final ByteBuf buffer, final org.joml.Quaterniondc quaternion) {
        buffer.writeDouble(quaternion.x());
        buffer.writeDouble(quaternion.y());
        buffer.writeDouble(quaternion.z());
        buffer.writeDouble(quaternion.w());
    }

    public static org.joml.Quaterniond read(final ByteBuf buffer, final org.joml.Quaterniond destination) {
        return destination.set(buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble());
    }
}
