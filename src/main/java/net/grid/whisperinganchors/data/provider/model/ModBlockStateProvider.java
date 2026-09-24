package net.grid.whisperinganchors.data.provider.model;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModBlocks;
import net.grid.whisperinganchors.common.world.block.AnchorPart;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, WhisperingAnchors.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        anchor(ModBlocks.WHISPERING_ANCHOR.get());
    }

    private void anchor(Block block) {
        ModelFile upper = models().getExistingFile(WhisperingAnchors.modLoc("block/whispering_anchor_upper"));
        ModelFile lower = models().getExistingFile(WhisperingAnchors.modLoc("block/whispering_anchor_lower"));

        getVariantBuilder(block).forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(WhisperingAnchorBlock.PART) == AnchorPart.TOP ? upper : lower)
                .build(), WhisperingAnchorBlock.FACING);
    }
}
