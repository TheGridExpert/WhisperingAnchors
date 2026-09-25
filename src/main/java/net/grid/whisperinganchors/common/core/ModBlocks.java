package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.stream.Collectors;

public class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WhisperingAnchors.MODID);

    public static final DeferredBlock<WhisperingAnchorBlock> WHISPERING_ANCHOR = BLOCKS.registerBlock("whispering_anchor", WhisperingAnchorBlock::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(25.0F, 1200.0F)
            .sound(SoundType.HEAVY_CORE)
            .noOcclusion()
            .dynamicShape()
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
    );

    public static Iterable<Block> getKnownBlocks() {
        return BLOCKS.getEntries().stream().map(DeferredHolder::get).collect(Collectors.toUnmodifiableSet());
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
