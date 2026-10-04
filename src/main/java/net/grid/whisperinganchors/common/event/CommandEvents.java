package net.grid.whisperinganchors.common.event;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.command.ExtractLootCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = WhisperingAnchors.MODID)
public class CommandEvents {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        ExtractLootCommand.register(event.getDispatcher());
    }
}
