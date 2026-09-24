package net.grid.whisperinganchors.common.core;

import com.mojang.serialization.MapCodec;
import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.world.structure.TraversalTowerStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModStructureTypes {
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, WhisperingAnchors.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<TraversalTowerStructure>> TRAVERSAL_TOWER = register("traversal_tower", TraversalTowerStructure.CODEC);

    private static <S extends Structure> DeferredHolder<StructureType<?>, StructureType<S>> register(String name, MapCodec<S> codec) {
        return STRUCTURE_TYPES.register(name, () -> () -> codec);
    }

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
    }
}
