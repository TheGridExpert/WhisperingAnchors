package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.world.blockentity.WhisperingAnchorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WhisperingAnchors.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WhisperingAnchorBlockEntity>> WHISPERING_ANCHOR = BLOCK_ENTITIES.register("whispering_anchor", () -> BlockEntityType.Builder.of(WhisperingAnchorBlockEntity::new, ModBlocks.WHISPERING_ANCHOR.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
