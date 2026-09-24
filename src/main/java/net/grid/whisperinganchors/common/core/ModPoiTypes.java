package net.grid.whisperinganchors.common.core;

import com.google.common.collect.ImmutableSet;
import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModPoiTypes {
    private static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, WhisperingAnchors.MODID);

    public static final DeferredHolder<PoiType, PoiType> WHISPERING_ANCHOR = POI_TYPES.register("whispering_anchor", () -> new PoiType(ImmutableSet.copyOf(ModBlocks.WHISPERING_ANCHOR.get().getStateDefinition().getPossibleStates()), 0, 1));

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
    }
}
