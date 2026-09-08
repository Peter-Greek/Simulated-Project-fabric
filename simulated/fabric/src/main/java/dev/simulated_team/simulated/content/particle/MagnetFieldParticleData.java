package dev.simulated_team.simulated.content.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import dev.simulated_team.simulated.index.SimParticleTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.StreamCodec;

public class MagnetFieldParticleData implements ParticleOptions, ICustomParticleDataWithSprite<MagnetFieldParticleData> {
    public static final MapCodec<MagnetFieldParticleData> CODEC = RecordCodecBuilder.mapCodec((i) -> {
        return i.group(Codec.BOOL.fieldOf("negative").forGetter((p) -> {
            return p.negative;
        })).apply(i, MagnetFieldParticleData::new);
    });
    public static final StreamCodec<ByteBuf, MagnetFieldParticleData> STREAM_CODEC;
    private boolean negative;

    public MagnetFieldParticleData(final boolean negative) {
        this.negative = negative;
    }

    public MagnetFieldParticleData() {
        this.negative = false;
    }

    public ParticleType<?> getType() {
        return SimParticleTypes.MAGNET_FIELD.get();
    }

    public Codec<MagnetFieldParticleData> getCodec(final ParticleType<MagnetFieldParticleData> type) {
        return CODEC.codec();
    }

    public ParticleEngine.SpriteParticleRegistration<MagnetFieldParticleData> getMetaFactory() {
        return MagnetFieldParticle.Factory::new;
    }


    static {
        STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, (p) -> p.negative, MagnetFieldParticleData::new);
    }

    public boolean isNegative() {
        return this.negative;
    }

    public void setNegative(final boolean negative) {
        this.negative = negative;
    }

    @Override
    public ParticleOptions.Deserializer<MagnetFieldParticleData> getDeserializer() {
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

