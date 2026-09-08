package dev.simulated_team.simulated.content.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;

/**
 * The serialisation 1.20.1 asks of a custom particle.
 *
 * <p>1.20.5 gave {@code ParticleOptions} a codec and a stream codec and dropped
 * everything else. 1.20.1 wants a {@code Deserializer} that parses from a
 * command string and from the network, plus a {@code writeToString}. Upstream's
 * codecs already describe the data, so these are written in terms of them —
 * the wire and command shapes are the codec's, so a particle spawned by command
 * reads the same JSON upstream would write.
 */
public final class SimParticleSerialization {

    private SimParticleSerialization() {
    }

    public static <T extends ParticleOptions> ParticleOptions.Deserializer<T> deserializer(
            final Codec<T> codec, final StreamCodec<? super ByteBuf, T> streamCodec) {
        return new ParticleOptions.Deserializer<>() {
            @Override
            public T fromCommand(final ParticleType<T> type, final StringReader reader)
                    throws CommandSyntaxException {
                reader.skipWhitespace();
                final com.google.gson.JsonElement json = reader.canRead()
                        ? com.google.gson.JsonParser.parseString(reader.getRemaining())
                        : new com.google.gson.JsonObject();
                reader.setCursor(reader.getTotalLength());
                return codec.parse(JsonOps.INSTANCE, json).result().orElseThrow(
                        () -> new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
                                net.minecraft.network.chat.Component.literal(
                                        "Malformed particle options")).createWithContext(reader));
            }

            @Override
            public T fromNetwork(final ParticleType<T> type, final FriendlyByteBuf buffer) {
                return streamCodec.decode(buffer);
            }
        };
    }

    public static <T extends ParticleOptions> void write(
            final StreamCodec<? super ByteBuf, T> streamCodec, final FriendlyByteBuf buffer, final T options) {
        streamCodec.encode(buffer, options);
    }

    public static <T extends ParticleOptions> String toString(final Codec<T> codec, final T options) {
        final String id = String.valueOf(BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType()));
        return codec.encodeStart(JsonOps.INSTANCE, options)
                .result()
                .map(json -> id + " " + json)
                .orElse(id);
    }
}
