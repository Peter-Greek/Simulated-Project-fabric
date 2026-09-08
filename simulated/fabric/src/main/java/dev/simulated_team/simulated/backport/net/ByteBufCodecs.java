package dev.simulated_team.simulated.backport.net;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * Stand-in for {@code net.minecraft.network.codec.ByteBufCodecs} (1.20.5+).
 *
 * <p>Only the members Simulated actually uses are here. The stream codecs
 * vanilla hangs off its own types rather than this class live in
 * {@link SimCodecs}.
 */
public final class ByteBufCodecs {

    private ByteBufCodecs() {
    }

    public static final StreamCodec<ByteBuf, Boolean> BOOL =
            StreamCodec.of((buf, value) -> buf.writeBoolean(value), ByteBuf::readBoolean);

    public static final StreamCodec<ByteBuf, Integer> INT = StreamCodec.of(
            (buf, value) -> new FriendlyByteBuf(buf).writeVarInt(value),
            buf -> new FriendlyByteBuf(buf).readVarInt());

    public static final StreamCodec<ByteBuf, Float> FLOAT =
            StreamCodec.of((buf, value) -> buf.writeFloat(value), ByteBuf::readFloat);

    public static final StreamCodec<ByteBuf, Double> DOUBLE =
            StreamCodec.of((buf, value) -> buf.writeDouble(value), ByteBuf::readDouble);

    public static final StreamCodec<ByteBuf, Long> VAR_LONG = StreamCodec.of(
            (buf, value) -> new FriendlyByteBuf(buf).writeVarLong(value),
            buf -> new FriendlyByteBuf(buf).readVarLong());

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

    /**
     * Vanilla routes a {@link Codec} through NBT to reach the wire. Doing the
     * same keeps the encoding identical to what upstream produces.
     */
    public static <T> StreamCodec<ByteBuf, T> fromCodec(final Codec<T> codec) {
        return StreamCodec.of(
                (buf, value) -> {
                    // 1.20.1's buffer writes a compound, so the encoded value is
                    // wrapped in one under a fixed key rather than written bare.
                    final Tag tag = codec.encodeStart(NbtOps.INSTANCE, value)
                            .getOrThrow(false, message -> {
                                throw new EncoderException("Failed to encode: " + message + " " + value);
                            });
                    final CompoundTag wrapper = new CompoundTag();
                    wrapper.put("v", tag);
                    new FriendlyByteBuf(buf).writeNbt(wrapper);
                },
                buf -> {
                    final CompoundTag wrapper = new FriendlyByteBuf(buf).readNbt();
                    if (wrapper == null) {
                        throw new DecoderException("Missing codec payload");
                    }
                    final Tag tag = wrapper.get("v");
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
                    if (size < 0 || size > 65536 || size > asByteBuf(buf).readableBytes()) {
                        throw new DecoderException("Invalid collection size");
                    }
                    final C values = factory.apply(size);
                    for (int i = 0; i < size; i++) {
                        values.add(elementCodec.decode(buf));
                    }
                    return values;
                });
    }

    /** The {@code .apply(list())} operation form: turns an element codec into a list codec. */
    public static <B, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
        return codec -> collection(ArrayList::new, codec);
    }

    /**
     * As {@link #list()}, with vanilla's decode-side size cap. Upstream uses it
     * for fixed-arity vectors, where an oversized list is a malformed packet.
     */
    public static <B, V> StreamCodec.CodecOperation<B, V, List<V>> list(final int maxSize) {
        return codec -> StreamCodec.of(
                (buf, values) -> {
                    if (values.size() > maxSize) {
                        throw new EncoderException("List too large: " + values.size() + " > " + maxSize);
                    }
                    new FriendlyByteBuf(asByteBuf(buf)).writeVarInt(values.size());
                    for (final V value : values) {
                        codec.encode(buf, value);
                    }
                },
                buf -> {
                    final int size = new FriendlyByteBuf(asByteBuf(buf)).readVarInt();
                    if (size < 0 || size > 65536 || size > asByteBuf(buf).readableBytes()) {
                        throw new DecoderException("Invalid collection size");
                    }
                    if (size > maxSize) {
                        throw new DecoderException("List too large: " + size + " > " + maxSize);
                    }
                    final List<V> values = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        values.add(codec.decode(buf));
                    }
                    return values;
                });
    }

    public static <B, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
            final IntFunction<? extends M> factory,
            final StreamCodec<? super B, K> keyCodec,
            final StreamCodec<? super B, V> valueCodec) {
        return StreamCodec.of(
                (buf, values) -> {
                    new FriendlyByteBuf(asByteBuf(buf)).writeVarInt(values.size());
                    values.forEach((key, value) -> {
                        keyCodec.encode(buf, key);
                        valueCodec.encode(buf, value);
                    });
                },
                buf -> {
                    final int size = new FriendlyByteBuf(asByteBuf(buf)).readVarInt();
                    if (size < 0 || size > 65536 || size > asByteBuf(buf).readableBytes()) {
                        throw new DecoderException("Invalid collection size");
                    }
                    final M values = factory.apply(size);
                    for (int i = 0; i < size; i++) {
                        values.put(keyCodec.decode(buf), valueCodec.decode(buf));
                    }
                    return values;
                });
    }

    /**
     * Some codecs are parameterised over {@code ByteBuf} and others over
     * {@link RegistryFriendlyByteBuf}. Both are netty buffers at runtime, so the
     * generic helpers above reach the primitive codecs through this cast rather
     * than being duplicated per buffer type.
     */
    private static ByteBuf asByteBuf(final Object buffer) {
        if (buffer instanceof ByteBuf byteBuf) {
            return byteBuf;
        }
        throw new IllegalArgumentException("Backport codecs require a netty ByteBuf, got " + buffer);
    }
}
