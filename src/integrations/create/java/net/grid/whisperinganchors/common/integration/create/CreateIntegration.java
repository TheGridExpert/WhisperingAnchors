package net.grid.whisperinganchors.common.integration.create;

import net.grid.whisperinganchors.common.integration.ModIntegration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public class CreateIntegration implements ModIntegration {

    @Override
    public String getModId() {
        return "create";
    }

    @Override
    public void initialize(IEventBus modBus) {
        modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(AnchorMovementBehaviour::register));
    }
}
