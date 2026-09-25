package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.world.particle.HarkeningSoulParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, WhisperingAnchors.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ANCHOR_CRUMBLE = PARTICLE_TYPES.register("anchor_crumble", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, ParticleType<HarkeningSoulParticleOption>> HARKENING_SOUL = PARTICLE_TYPES.register("harkening_soul", HarkeningSoulParticleOption::type);

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
