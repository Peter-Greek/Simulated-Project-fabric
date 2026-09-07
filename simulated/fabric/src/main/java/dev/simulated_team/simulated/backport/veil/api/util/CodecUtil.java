package dev.simulated_team.simulated.backport.veil.api.util;

import com.mojang.serialization.Codec;
import org.joml.Vector3d;

import java.util.List;

/** Small codecs Veil ships. Real: plain codec composition. */
public final class CodecUtil {

    /**
     * A three-component double vector, as a list, matching Veil's encoding.
     * Upstream reaches for it under both names.
     */
    public static final Codec<Vector3d> VECTOR3D_CODEC = vector();

    public static final Codec<Vector3d> VECTOR = vector();

    private static Codec<Vector3d> vector() {
        return Codec.DOUBLE.listOf().comapFlatMap(
            values -> values.size() == 3
                    ? com.mojang.serialization.DataResult.success(
                            new Vector3d(values.get(0), values.get(1), values.get(2)))
                    : com.mojang.serialization.DataResult.error(() -> "Expected 3 elements"),
            vector -> List.of(vector.x, vector.y, vector.z));
    }

    private CodecUtil() {
    }
}
