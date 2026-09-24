package net.grid.whisperinganchors.data.provider.loot;

import net.grid.whisperinganchors.common.core.ModBlocks;
import net.grid.whisperinganchors.misc.mixin.accessor.VanillaBlockLootAccessor;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

public class ModBlockLootProvider extends BlockLootSubProvider {

    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(VanillaBlockLootAccessor.getExplosionResistantBlocks(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        //add(ModBlocks.WHISPERING_ANCHOR.get(), noDrop());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.getKnownBlocks();
    }
}
