package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.world.advancement.criterion.HarkeningTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAdvancements {
    private static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES = DeferredRegister.create(BuiltInRegistries.TRIGGER_TYPES, WhisperingAnchors.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, HarkeningTrigger> HARKENED_TO_ANCHOR = TRIGGER_TYPES.register("harkened_to_anchor", HarkeningTrigger::new);

    public static void register(IEventBus eventBus) {
        TRIGGER_TYPES.register(eventBus);
    }
}
