package dev.simulated_team.simulated.backport.catnip;

import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Stand-in for {@code net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders}.
 * See {@link CatnipStreamCodecs}.
 */
public final class CatnipStreamCodecBuilders {

    private CatnipStreamCodecBuilders() {
    }

    public static <B, V> StreamCodec<B, List<V>> list(final StreamCodec<? super B, V> elementCodec) {
        return ByteBufCodecs.collection(ArrayList::new, elementCodec);
    }

    public static <E extends Enum<E>> StreamCodec<ByteBuf, E> ofEnum(final Class<E> type) {
        return StreamCodec.of(
                (buf, value) -> new FriendlyByteBuf(buf).writeEnum(value),
                buf -> new FriendlyByteBuf(buf).readEnum(type));
    }
}
