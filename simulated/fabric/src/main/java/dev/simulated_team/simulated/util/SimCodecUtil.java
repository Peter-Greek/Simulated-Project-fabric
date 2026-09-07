package dev.simulated_team.simulated.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import io.netty.buffer.ByteBuf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import dev.simulated_team.simulated.backport.net.SimCodecs;
import dev.simulated_team.simulated.backport.physics.api.physics.force.ForceGroup;
import dev.simulated_team.simulated.backport.physics.api.physics.force.ForceGroups;
import dev.simulated_team.simulated.backport.physics.api.physics.force.QueuedForceGroup;
import dev.simulated_team.simulated.backport.physics.companion.math.BoundingBox3d;

/**
 * The codecs upstream's packets are built from. The Sable-typed ones read and
 * write the stand-in types under {@code backport.physics}; the shapes are
 * upstream's, so the wire format is unchanged when the real engine lands.
 */
public class SimCodecUtil {

    public static final StreamCodec<ByteBuf, Vector3d> STREAM_VECTOR3D = StreamCodec.of(
            (buf, value) -> {
                buf.writeDouble(value.x());
                buf.writeDouble(value.y());
                buf.writeDouble(value.z());
            },
            buf -> new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble()));

    public static final StreamCodec<ByteBuf, Vector3dc> STREAM_VECTOR3DC = ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list(3))
            .map(l -> new Vector3d(l.get(0), l.get(1), l.get(2)), (v) -> List.of(v.x(), v.y(), v.z()));

    public static final StreamCodec<ByteBuf, BoundingBox3d> BOUNDING_BOX_3D_STREAM_CODEC =
            ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list(6))
                    .map(l -> new BoundingBox3d(l.get(0), l.get(1), l.get(2), l.get(3), l.get(4), l.get(5)),
                            bb -> List.of(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ));

    public static final StreamCodec<ByteBuf, ForceGroup> STREAM_FORCE_GROUP =
            SimCodecs.RESOURCE_LOCATION.map(ForceGroups.REGISTRY::get, ForceGroups.REGISTRY::getKey);

    public static final StreamCodec<ByteBuf, QueuedForceGroup.PointForce> STREAM_POINT_FORCE =
            STREAM_VECTOR3DC.apply(ByteBufCodecs.list(2))
                    .map(l -> new QueuedForceGroup.PointForce(l.get(0), l.get(1)),
                            p -> List.of(p.point(), p.force()));

    public static <T> Codec<T> withAlternative(final Codec<T> first, final Codec<T> second) {
        return new WithAlternativeButGood<>(first, second);
    }

    private record WithAlternativeButGood<T>(Codec<T> first, Codec<T> second) implements Codec<T> {
        @Override
        public <T1> DataResult<Pair<T, T1>> decode(final DynamicOps<T1> ops, final T1 input) {
            final DataResult<Pair<T, T1>> result = this.first.decode(ops, input);
            if (result.result().isPresent()) {
                return result;
            }
            return this.second.decode(ops, input);
        }

        @Override
        public <T1> DataResult<T1> encode(final T input, final DynamicOps<T1> ops, final T1 prefix) {
            final DataResult<T1> result = this.first.encode(input, ops, prefix);
            if (result.result().isPresent()) {
                return result;
            }
            return this.second.encode(input, ops, prefix);
        }
    }
}
