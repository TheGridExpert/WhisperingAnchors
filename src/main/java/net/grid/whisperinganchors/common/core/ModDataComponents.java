package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.world.item.component.AnchorAttunement;
import net.grid.whisperinganchors.common.world.item.component.KnownAnchors;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, WhisperingAnchors.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AnchorAttunement>> ANCHOR_ATTUNEMENT = DATA_COMPONENTS.register("anchor_attunement", () -> new DataComponentType.Builder<AnchorAttunement>().persistent(AnchorAttunement.CODEC).networkSynchronized(AnchorAttunement.STREAM_CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<KnownAnchors>> KNOWN_ANCHORS = DATA_COMPONENTS.register("known_anchors", () -> new DataComponentType.Builder<KnownAnchors>().persistent(KnownAnchors.CODEC).networkSynchronized(KnownAnchors.STREAM_CODEC).build());

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
