package dev.simulated_team.simulated.backport.physics.companion.math;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * Conversions between Minecraft's vector types and JOML's.
 *
 * <p>Pure geometry with no engine behind it, so this is a real implementation
 * rather than a stand-in — see the package documentation.
 */
public final class JOMLConversion {

    public static final Vector3dc ZERO = new Vector3d();

    public static final Quaterniondc QUAT_IDENTITY = new Quaterniond();

    private JOMLConversion() {
    }

    public static Vector3d toJOML(final Vec3 vec) {
        return new Vector3d(vec.x, vec.y, vec.z);
    }

    public static Vector3d toJOML(final Vec3 vec, final Vector3d destination) {
        return destination.set(vec.x, vec.y, vec.z);
    }

    public static Vector3d toJOML(final net.minecraft.core.Position position) {
        return new Vector3d(position.x(), position.y(), position.z());
    }

    public static Vector3i toJOML(final Vec3i vec) {
        return new Vector3i(vec.getX(), vec.getY(), vec.getZ());
    }

    public static Vector3d toJOML(final Vector3f vec) {
        return new Vector3d(vec.x(), vec.y(), vec.z());
    }

    public static Quaterniond toJOML(final Quaternionf quaternion) {
        return new Quaterniond(quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w());
    }

    public static BoundingBox3d toJOML(final AABB box) {
        return new BoundingBox3d(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public static Vec3 toMojang(final Vector3dc vec) {
        return new Vec3(vec.x(), vec.y(), vec.z());
    }

    public static Vec3 toMojang(final Vector3f vec) {
        return new Vec3(vec.x(), vec.y(), vec.z());
    }

    public static Quaternionf toMojang(final Quaterniondc quaternion) {
        return new Quaternionf((float) quaternion.x(), (float) quaternion.y(),
                (float) quaternion.z(), (float) quaternion.w());
    }

    public static AABB toMojang(final BoundingBox3dc box) {
        return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    /** The centre of the block at this position, as a JOML vector. */
    public static Vector3d atCenterOf(final Vec3i pos) {
        return new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static Vector3d atCenterOf(final Vec3i pos, final Vector3d destination) {
        return destination.set(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /** The lower corner of the block at this position, as a JOML vector. */
    public static Vector3d atLowerCornerOf(final Vec3i pos) {
        return new Vector3d(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Vector3d atLowerCornerOf(final Vec3i pos, final Vector3d destination) {
        return destination.set(pos.getX(), pos.getY(), pos.getZ());
    }

    public static BlockPos toBlockPos(final Vector3dc vec) {
        return BlockPos.containing(vec.x(), vec.y(), vec.z());
    }
}
