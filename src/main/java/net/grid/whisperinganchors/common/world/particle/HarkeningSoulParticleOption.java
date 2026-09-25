package net.grid.whisperinganchors.common.world.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.grid.whisperinganchors.common.core.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record HarkeningSoulParticleOption(float radius, float spin) implements ParticleOptions {

    public static final MapCodec<HarkeningSoulParticleOption> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("radius").forGetter(HarkeningSoulParticleOption::radius),
            Codec.FLOAT.fieldOf("spin").forGetter(HarkeningSoulParticleOption::spin)
    ).apply(instance, HarkeningSoulParticleOption::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HarkeningSoulParticleOption> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, HarkeningSoulParticleOption::radius,
            ByteBufCodecs.FLOAT, HarkeningSoulParticleOption::spin,
            HarkeningSoulParticleOption::new
    );

    public static ParticleType<HarkeningSoulParticleOption> type() {
        return new ParticleType<>(false) {
            @Override
            public MapCodec<HarkeningSoulParticleOption> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, HarkeningSoulParticleOption> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.HARKENING_SOUL.get();
    }
}
