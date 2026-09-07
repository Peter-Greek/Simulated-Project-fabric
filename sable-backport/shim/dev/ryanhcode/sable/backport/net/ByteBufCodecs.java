package dev.ryanhcode.sable.backport.net;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;

/**
 * Stand-in for {@code net.minecraft.network.codec.ByteBufCodecs} (1.20.5+).
 *
 * <p>Also carries the handful of stream codecs that vanilla hangs off other
 * classes as static fields — {@code UUIDUtil.STREAM_CODEC} and
 * {@code ResourceLocation.STREAM_CODEC}. A shim cannot add fields to vanilla
 * types, so those six references are rewritten to point here instead.
 */
public final class ByteBufCodecs {

    private ByteBufCodecs() {
    }

    public static final StreamCodec<ByteBuf, Boolean> BOOL =
            StreamCodec.of((buf, value) -> buf.writeBoolean(value), ByteBuf::readBoolean);

    public static final StreamCodec<ByteBuf, Integer> INT =
            StreamCodec.of((buf, value) -> buf.writeInt(value), ByteBuf::readInt);

    public static final StreamCodec<ByteBuf, Float> FLOAT =
            StreamCodec.of((buf, value) -> buf.writeFloat(value), ByteBuf::readFloat);

    public static final StreamCodec<ByteBuf, Double> DOUBLE =
            StreamCodec.of((buf, value) -> buf.writeDouble(value), ByteBuf::readDouble);

    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = StreamCodec.of(
            (buf, value) -> new FriendlyByteBuf(buf).writeUtf(value),
            buf -> new FriendlyByteBuf(buf).readUtf());

    public static final StreamCodec<ByteBuf, Vector3f> VECTOR3F = StreamCodec.of(
            (buf, value) -> {
                buf.writeFloat(value.x());
                buf.writeFloat(value.y());
                buf.writeFloat(value.z());
            },
            buf -> new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()));

    /** Replaces {@code UUIDUtil.STREAM_CODEC}, which is a 1.20.5+ field. */
    public static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeLong(value.getMostSignificantBits());
                buf.writeLong(value.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong()));

    /** Replaces {@code ResourceLocation.STREAM_CODEC}, which is a 1.20.5+ field. */
    public static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION = StreamCodec.of(
            (buf, value) -> new FriendlyByteBuf(buf).writeResourceLocation(value),
            buf -> new FriendlyByteBuf(buf).readResourceLocation());

    /**
     * Vanilla routes a {@link Codec} through NBT to reach the wire. Doing the
     * same keeps the encoding identical to what upstream Sable produces, so a
     * 1.20.1 client and a 1.21.1 client would agree on the bytes.
     */
    public static <T> StreamCodec<ByteBuf, T> fromCodec(final Codec<T> codec) {
        return StreamCodec.of(
                (buf, value) -> {
                    final Tag tag = codec.encodeStart(NbtOps.INSTANCE, value)
                            .getOrThrow(false, message -> {
                                throw new EncoderException("Failed to encode: " + message + " " + value);
                            });
                    new FriendlyByteBuf(buf).writeNbt(tag);
                },
                buf -> {
                    final Tag tag = new FriendlyByteBuf(buf).readNbt();
                    return codec.parse(NbtOps.INSTANCE, tag)
                            .getOrThrow(false, message -> {
                                throw new DecoderException("Failed to decode: " + message + " " + tag);
                            });
                });
    }

    public static <B, V> StreamCodec<B, Optional<V>> optional(final StreamCodec<B, V> codec) {
        return StreamCodec.of(
                (buf, value) -> {
                    BOOL.encode(asByteBuf(buf), value.isPresent());
                    value.ifPresent(present -> codec.encode(buf, present));
                },
                buf -> BOOL.decode(asByteBuf(buf))
                        ? Optional.of(codec.decode(buf))
                        : Optional.empty());
    }

    public static <B, V, C extends Collection<V>> StreamCodec<B, C> collection(
            final IntFunction<C> factory, final StreamCodec<? super B, V> elementCodec) {
        return StreamCodec.of(
                (buf, values) -> {
                    new FriendlyByteBuf(asByteBuf(buf)).writeVarInt(values.size());
                    for (final V value : values) {
                        elementCodec.encode(buf, value);
                    }
                },
                buf -> {
                    final int size = new FriendlyByteBuf(asByteBuf(buf)).readVarInt();
                    final C values = factory.apply(size);
                    for (int i = 0; i < size; i++) {
                        values.add(elementCodec.decode(buf));
                    }
                    return values;
                });
    }

    /**
     * Sable parameterises some codecs over {@code ByteBuf} and others over
     * {@code RegistryFriendlyByteBuf}. Both are netty buffers at runtime, so the
     * generic helpers above reach the primitive codecs through this cast rather
     * than being duplicated per buffer type.
     */
    private static ByteBuf asByteBuf(final Object buffer) {
        if (buffer instanceof ByteBuf byteBuf) {
            return byteBuf;
        }
        throw new IllegalArgumentException("Sable backport codecs require a netty ByteBuf, got " + buffer);
    }
}
