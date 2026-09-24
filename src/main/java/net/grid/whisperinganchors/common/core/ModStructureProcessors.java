package net.grid.whisperinganchors.common.core;

import com.mojang.serialization.MapCodec;
import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.world.structure.processor.RadialFadeProcessor;
import net.grid.whisperinganchors.common.world.structure.processor.WeatheringProcessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModStructureProcessors {
    private static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, WhisperingAnchors.MODID);

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<WeatheringProcessor>> WEATHERING = register("weathering", WeatheringProcessor.CODEC);
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<RadialFadeProcessor>> RADIAL_FADE = register("radial_fade", RadialFadeProcessor.CODEC);

    private static <P extends StructureProcessor> DeferredHolder<StructureProcessorType<?>, StructureProcessorType<P>> register(String name, MapCodec<P> codec) {
        return STRUCTURE_PROCESSORS.register(name, () -> () -> codec);
    }

    public static void register(IEventBus eventBus) {
        STRUCTURE_PROCESSORS.register(eventBus);
    }
}
