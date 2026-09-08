package dev.simulated_team.simulated.content.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import dev.simulated_team.simulated.index.SimParticleTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Stolen shamelessly from {@link com.simibubi.create.content.kinetics.base.RotationIndicatorParticleData}
 */
public class AugerIndicatorParticleData implements ParticleOptions, ICustomParticleDataWithSprite<AugerIndicatorParticleData> {
    public static final MapCodec<AugerIndicatorParticleData> CODEC = RecordCodecBuilder.mapCodec(i -> i
            .group(Codec.INT.fieldOf("color")
                            .forGetter(p -> p.color),
                    Codec.FLOAT.fieldOf("speed")
                            .forGetter(p -> p.speed),
                    Codec.FLOAT.fieldOf("radius1")
                            .forGetter(p -> p.radius1),
                    Codec.FLOAT.fieldOf("radius2")
                            .forGetter(p -> p.radius2),
                    Codec.FLOAT.fieldOf("angle_offset")
                            .forGetter(p -> p.angleOffset),
                    Codec.INT.fieldOf("life_span")
                            .forGetter(p -> p.lifeSpan),
                    Direction.CODEC.fieldOf("direction")
                            .forGetter(p -> p.direction))
            .apply(i, AugerIndicatorParticleData::new));

    // lazy way but its past the maximum number of fields for the composite constructor and i cant be bothered figuring out how to solve it
    public static final StreamCodec<ByteBuf, AugerIndicatorParticleData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC.codec());

    public final int color;
    public final float speed;
    public final float radius1;
    public final float radius2;
    public float angleOffset;
    public final int lifeSpan;
    public final Direction direction;

    public AugerIndicatorParticleData(final int color, final float speed,
                                         final float radius1, final float radius2, final float angleOffset,
                                         final int lifeSpan,
                                         final Direction direction) {
        this.color = color;
        this.speed = speed;
        this.radius1 = radius1;
        this.radius2 = radius2;
        this.angleOffset = angleOffset;
        this.lifeSpan = lifeSpan;
        this.direction = direction;
    }

    public AugerIndicatorParticleData() {
        this(0, 0, 0, 0, 0, 0, Direction.NORTH);
    }

    @Override
    public ParticleType<?> getType() {
        return SimParticleTypes.AUGER_INDICATOR.get();
    }

    @Override
    public Codec<AugerIndicatorParticleData> getCodec(final ParticleType<AugerIndicatorParticleData> type) {
        return CODEC.codec();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public ParticleEngine.SpriteParticleRegistration<AugerIndicatorParticleData> getMetaFactory() {
        return AugerIndicatorParticle.Factory::new;
    }

    @Override
    public ParticleOptions.Deserializer<AugerIndicatorParticleData> getDeserializer() {
        return SimParticleSerialization.deserializer(CODEC.codec(), STREAM_CODEC);
    }

    @Override
    public void writeToNetwork(final net.minecraft.network.FriendlyByteBuf buffer) {
        SimParticleSerialization.write(STREAM_CODEC, buffer, this);
    }

    @Override
    public String writeToString() {
        return SimParticleSerialization.toString(CODEC.codec(), this);
    }

}
