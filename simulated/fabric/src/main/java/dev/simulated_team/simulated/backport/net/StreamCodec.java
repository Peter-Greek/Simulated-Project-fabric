package dev.simulated_team.simulated.backport.net;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Stand-in for {@code net.minecraft.network.codec.StreamCodec}, which arrived in
 * 1.20.5. Every Sable packet is built on it, so the type has to exist before
 * anything under {@code network/} compiles on 1.20.1.
 *
 * <p>The shape is copied from vanilla so the call sites are unchanged and only
 * the import is rewritten. Only the members Sable actually uses are here; the
 * measured list is in the plan notes log.
 */
public interface StreamCodec<B, V> {

    V decode(B buffer);

    void encode(B buffer, V value);

    @FunctionalInterface
    interface Decoder<B, V> {
        V decode(B buffer);
    }

    @FunctionalInterface
    interface Encoder<B, V> {
        void encode(B buffer, V value);
    }

    /** The member-encoder form: the value writes itself into the buffer. */
    @FunctionalInterface
    interface MemberEncoder<B, V> {
        void encode(V value, B buffer);
    }

    @FunctionalInterface
    interface CodecOperation<B, S, T> {
        StreamCodec<B, T> apply(StreamCodec<B, S> codec);
    }

    default <T> StreamCodec<B, T> apply(final CodecOperation<B, V, T> operation) {
        return operation.apply(this);
    }

    default <T> StreamCodec<B, T> map(final Function<? super V, ? extends T> to,
                                      final Function<? super T, ? extends V> from) {
        final StreamCodec<B, V> self = this;
        return new StreamCodec<B, T>() {
            @Override
            public T decode(final B buffer) {
                return to.apply(self.decode(buffer));
            }

            @Override
            public void encode(final B buffer, final T value) {
                self.encode(buffer, from.apply(value));
            }
        };
    }

    static <B, V> StreamCodec<B, V> of(final Encoder<B, V> encoder, final Decoder<B, V> decoder) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(final B buffer) {
                return decoder.decode(buffer);
            }

            @Override
            public void encode(final B buffer, final V value) {
                encoder.encode(buffer, value);
            }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(final MemberEncoder<B, V> encoder, final Decoder<B, V> decoder) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(final B buffer) {
                return decoder.decode(buffer);
            }

            @Override
            public void encode(final B buffer, final V value) {
                encoder.encode(value, buffer);
            }
        };
    }

    static <B, V> StreamCodec<B, V> unit(final V value) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(final B buffer) {
                return value;
            }

            @Override
            public void encode(final B buffer, final V ignored) {
            }
        };
    }

    @FunctionalInterface
    interface Function3<T1, T2, T3, R> {
        R apply(T1 t1, T2 t2, T3 t3);
    }

    @FunctionalInterface
    interface Function4<T1, T2, T3, T4, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4);
    }

    @FunctionalInterface
    interface Function5<T1, T2, T3, T4, T5, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
    }

    @FunctionalInterface
    interface Function6<T1, T2, T3, T4, T5, T6, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);
    }

    static <B, C, T1> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1,
            final Function<T1, C> constructor) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(final B buffer) {
                return constructor.apply(codec1.decode(buffer));
            }

            @Override
            public void encode(final B buffer, final C value) {
                codec1.encode(buffer, getter1.apply(value));
            }
        };
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1,
            final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2,
            final BiFunction<T1, T2, C> constructor) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(final B buffer) {
                return constructor.apply(codec1.decode(buffer), codec2.decode(buffer));
            }

            @Override
            public void encode(final B buffer, final C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
            }
        };
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1,
            final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2,
            final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3,
            final Function3<T1, T2, T3, C> constructor) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(final B buffer) {
                return constructor.apply(codec1.decode(buffer), codec2.decode(buffer), codec3.decode(buffer));
            }

            @Override
            public void encode(final B buffer, final C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
            }
        };
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1,
            final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2,
            final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3,
            final StreamCodec<? super B, T4> codec4, final Function<C, T4> getter4,
            final Function4<T1, T2, T3, T4, C> constructor) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(final B buffer) {
                return constructor.apply(codec1.decode(buffer), codec2.decode(buffer),
                        codec3.decode(buffer), codec4.decode(buffer));
            }

            @Override
            public void encode(final B buffer, final C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1,
            final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2,
            final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3,
            final StreamCodec<? super B, T4> codec4, final Function<C, T4> getter4,
            final StreamCodec<? super B, T5> codec5, final Function<C, T5> getter5,
            final Function5<T1, T2, T3, T4, T5, C> constructor) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(final B buffer) {
                return constructor.apply(codec1.decode(buffer), codec2.decode(buffer),
                        codec3.decode(buffer), codec4.decode(buffer), codec5.decode(buffer));
            }

            @Override
            public void encode(final B buffer, final C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1,
            final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2,
            final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3,
            final StreamCodec<? super B, T4> codec4, final Function<C, T4> getter4,
            final StreamCodec<? super B, T5> codec5, final Function<C, T5> getter5,
            final StreamCodec<? super B, T6> codec6, final Function<C, T6> getter6,
            final Function6<T1, T2, T3, T4, T5, T6, C> constructor) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(final B buffer) {
                return constructor.apply(codec1.decode(buffer), codec2.decode(buffer),
                        codec3.decode(buffer), codec4.decode(buffer),
                        codec5.decode(buffer), codec6.decode(buffer));
            }

            @Override
            public void encode(final B buffer, final C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
            }
        };
    }
}
