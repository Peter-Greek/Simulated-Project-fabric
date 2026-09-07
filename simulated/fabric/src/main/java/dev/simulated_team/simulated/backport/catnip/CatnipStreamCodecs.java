package dev.simulated_team.simulated.backport.catnip;

import dev.simulated_team.simulated.backport.net.StreamCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/**
 * Stand-in for {@code net.createmod.catnip.codecs.stream.CatnipStreamCodecs},
 * which Catnip only grew once stream codecs existed in vanilla. The two codecs
 * Simulated uses are written here against the backport's {@code StreamCodec},
 * with vanilla's own wire shapes.
 */
public final class CatnipStreamCodecs {

    private CatnipStreamCodecs() {
    }

    public static final StreamCodec<ByteBuf, Vec3> VEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeDouble(value.x);
                buf.writeDouble(value.y);
                buf.writeDouble(value.z);
            },
            buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));

    public static final StreamCodec<ByteBuf, InteractionHand> HAND = StreamCodec.of(
            (buf, value) -> new FriendlyByteBuf(buf).writeEnum(value),
            buf -> new FriendlyByteBuf(buf).readEnum(InteractionHand.class));
}
