package net.grid.whisperinganchors;

import net.grid.whisperinganchors.common.config.ModConfig;
import net.grid.whisperinganchors.common.core.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(WhisperingAnchors.MODID)
public class WhisperingAnchors {

    public static final String MODID = "whispering_anchors";

    public WhisperingAnchors(IEventBus eventBus, ModContainer modContainer) {
        ModBlocks.register(eventBus);
        ModDataComponents.register(eventBus);
        ModItems.register(eventBus);
        ModBlockEntities.register(eventBus);
        ModEntities.register(eventBus);
        ModParticles.register(eventBus);
        ModSounds.register(eventBus);
        ModPoiTypes.register(eventBus);
        ModStructureTypes.register(eventBus);
        ModStructureProcessors.register(eventBus);
        ModMapDecorations.register(eventBus);
        ModAdvancements.register(eventBus);

        modContainer.registerConfig(Type.SERVER, ModConfig.serverSpec);

        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
