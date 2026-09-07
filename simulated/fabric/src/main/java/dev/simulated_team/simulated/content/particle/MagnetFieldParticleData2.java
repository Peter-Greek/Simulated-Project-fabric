package dev.simulated_team.simulated.content.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import dev.simulated_team.simulated.index.SimParticleTypes;
import io.netty.buffer.ByteBuf;
import dev.simulated_team.simulated.backport.catnip.CatnipStreamCodecs;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import net.minecraft.world.phys.Vec3;

public class MagnetFieldParticleData2 implements ParticleOptions, ICustomParticleDataWithSprite<MagnetFieldParticleData2> {
    //public static final MapCodec<MagnetFieldParticleData2> CODEC = RecordCodecBuilder.mapCodec((i) -> {
    //    return i.group(Codec.BOOL.fieldOf("negative").forGetter((p) -> {
    //        return p.negative;
    //    })).apply(i, MagnetFieldParticleData2::new);
    //});

    public static final MapCodec<MagnetFieldParticleData2> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Vec3.CODEC.fieldOf("previous_offset").forGetter(p -> p.previousOffset),
            Vec3.CODEC.fieldOf("next_offset").forGetter(p -> p.nextOffset),
            Codec.BOOL.fieldOf("negative").forGetter(p -> p.negative),
            Codec.INT.fieldOf("timeUntilEnd").forGetter(p -> p.timeUntilEnd)
    ).apply(instance, MagnetFieldParticleData2::new));

    public static final StreamCodec<ByteBuf, MagnetFieldParticleData2> STREAM_CODEC;
    Vec3 previousOffset;
    Vec3 nextOffset;
    private boolean negative;
    private int timeUntilEnd;

    public MagnetFieldParticleData2(final Vec3 previousOffset, final Vec3 nextOffset, final boolean negative, final int timeUntilEnd) {
        this.negative = negative;
        this.previousOffset = previousOffset;
        this.nextOffset = nextOffset;
        this.timeUntilEnd = timeUntilEnd;
    }

    public MagnetFieldParticleData2() {
        this.negative = false;
    }

    public ParticleType<?> getType() {
        return SimParticleTypes.MAGNET_FIELD2.get();
    }

    public Codec<MagnetFieldParticleData2> getCodec(final ParticleType<MagnetFieldParticleData2> type) {
        return CODEC.codec();
    }

    public ParticleEngine.SpriteParticleRegistration<MagnetFieldParticleData2> getMetaFactory() {
        return MagnetFieldParticle2.Factory::new;
    }


    static {
        STREAM_CODEC = StreamCodec.composite(
                CatnipStreamCodecs.VEC, p -> p.previousOffset,
                CatnipStreamCodecs.VEC, p -> p.nextOffset,
                ByteBufCodecs.BOOL, p -> p.negative,
                ByteBufCodecs.INT,p -> p.timeUntilEnd,
                MagnetFieldParticleData2::new);
    }

    public boolean isNegative() {
        return this.negative;
    }

    public int getTimeUntilEnd()
    {
        return this.timeUntilEnd;
    }

    public void setNegative(final boolean negative) {
        this.negative = negative;
    }

    @Override
    public ParticleOptions.Deserializer<MagnetFieldParticleData2> getDeserializer() {
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
