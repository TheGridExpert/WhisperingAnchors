package net.grid.whisperinganchors.common.integration.create;

import net.grid.whisperinganchors.common.integration.ModIntegration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class CreateIntegration implements ModIntegration {

    @Override
    public String getModId() {
        return "create";
    }

    @Override
    public void initialize(IEventBus modBus) {
        modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(AnchorMovementBehaviour::register));
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1").optional()
                .playToServer(PunchContraptionAnchorPayload.TYPE, PunchContraptionAnchorPayload.STREAM_CODEC, PunchContraptionAnchorPayload::handle)
                .playToClient(ContraptionAnchorImpulsePayload.TYPE, ContraptionAnchorImpulsePayload.STREAM_CODEC, ContraptionAnchorImpulsePayload::handle));

        if (FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.addListener(ContraptionAnchorPunchHandler::punchAnchor);
            NeoForge.EVENT_BUS.addListener(ContraptionAnchorPunchHandler::resetClick);
        }
    }
}
