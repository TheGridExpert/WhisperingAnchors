package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.common.network.BellUseDurationPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class ModPayloads {

    public static void register(IEventBus eventBus) {
        eventBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1")
                .playToClient(BellUseDurationPayload.TYPE, BellUseDurationPayload.STREAM_CODEC, BellUseDurationPayload::handle));
    }
}
