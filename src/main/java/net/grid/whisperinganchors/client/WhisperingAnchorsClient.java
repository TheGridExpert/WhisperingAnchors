package net.grid.whisperinganchors.client;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.client.extension.HarkeningBellClientExtensions;
import net.grid.whisperinganchors.client.extension.HeedingCompassClientExtensions;
import net.grid.whisperinganchors.client.extension.WhisperingAnchorClientExtensions;
import net.grid.whisperinganchors.client.model.AnchorRenderSettings;
import net.grid.whisperinganchors.client.model.FreeAngleModel;
import net.grid.whisperinganchors.client.particle.AnchorCrumbleParticle;
import net.grid.whisperinganchors.client.particle.HarkeningSoulParticle;
import net.grid.whisperinganchors.client.renderer.BellGlint;
import net.grid.whisperinganchors.client.renderer.BellOverlay;
import net.grid.whisperinganchors.client.renderer.HeedingCompassAngle;
import net.grid.whisperinganchors.client.renderer.blockentity.WhisperingAnchorRenderer;
import net.grid.whisperinganchors.client.renderer.entity.AnchorEntityRenderer;
import net.grid.whisperinganchors.common.core.ModBlockEntities;
import net.grid.whisperinganchors.common.core.ModBlocks;
import net.grid.whisperinganchors.common.core.ModEntities;
import net.grid.whisperinganchors.common.core.ModDataComponents;
import net.grid.whisperinganchors.common.core.ModItems;
import net.grid.whisperinganchors.common.core.ModParticles;
import net.grid.whisperinganchors.common.world.item.HarkeningBellItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = WhisperingAnchors.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = WhisperingAnchors.MODID, value = Dist.CLIENT)
public final class WhisperingAnchorsClient {

    public static final ModelResourceLocation ANCHOR_BODY_MODEL = ModelResourceLocation.standalone(WhisperingAnchors.modLoc("block/anchor_body"));
    public static final ModelResourceLocation ANCHOR_CHAIN_MODEL = ModelResourceLocation.standalone(WhisperingAnchors.modLoc("block/anchor_chain"));

    public WhisperingAnchorsClient(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ANCHOR_CHAIN_MODEL);
        event.register(BellOverlay.MODEL);
    }

    @SubscribeEvent
    public static void bakeAdditionalModels(ModelEvent.ModifyBakingResult event) {
        AnchorRenderSettings.reload(Minecraft.getInstance().getResourceManager());
        BakedModel baked = FreeAngleModel.bake(Minecraft.getInstance().getResourceManager(), event.getTextureGetter(), ANCHOR_BODY_MODEL.id());
        if (baked != null) {
            event.getModels().put(ANCHOR_BODY_MODEL, baked);
        }
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ANCHOR.get(), AnchorEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WHISPERING_ANCHOR.get(), WhisperingAnchorRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpecial(ModParticles.ANCHOR_CRUMBLE.get(), new AnchorCrumbleParticle.Provider());
        event.registerSpriteSet(ModParticles.HARKENING_SOUL.get(), HarkeningSoulParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerRenderBuffers(RegisterRenderBuffersEvent event) {
        event.registerRenderBuffer(BellGlint.RENDER_TYPE);
        event.registerRenderBuffer(BellOverlay.RENDER_TYPE);
    }

    @SubscribeEvent
    public static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ModItems.HARKENING_BELL.get(), HarkeningBellItem.ATTUNED_PROPERTY, (stack, level, entity, seed) -> stack.has(ModDataComponents.ANCHOR_ATTUNEMENT) ? 1.0F : 0.0F);
            ItemProperties.register(ModItems.HARKENING_BELL.get(), HarkeningBellItem.RINGING_PROPERTY, (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            ItemProperties.register(ModItems.HEEDING_COMPASS.get(), ResourceLocation.withDefaultNamespace("angle"), new HeedingCompassAngle());
        });
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerBlock(new WhisperingAnchorClientExtensions(), ModBlocks.WHISPERING_ANCHOR.get());
        event.registerItem(new HarkeningBellClientExtensions(), ModItems.HARKENING_BELL.get());
        event.registerItem(new HeedingCompassClientExtensions(), ModItems.HEEDING_COMPASS.get());
    }
}
