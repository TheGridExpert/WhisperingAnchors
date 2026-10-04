package net.grid.whisperinganchors.common.integration;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

import java.util.ServiceLoader;

public interface ModIntegration {

    String getModId();

    void initialize(IEventBus modBus);

    static void initializeAll(IEventBus modBus) {
        for (ModIntegration integration : ServiceLoader.load(ModIntegration.class, ModIntegration.class.getClassLoader())) {
            if (ModList.get().isLoaded(integration.getModId())) {
                integration.initialize(modBus);
            }
        }
    }
}
