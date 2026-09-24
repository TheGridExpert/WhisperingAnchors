package net.grid.whisperinganchors.data.provider.tag;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModBlocks;
import net.grid.whisperinganchors.common.core.tag.ModBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, WhisperingAnchors.MODID, existingFileHelper);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModBlockTags.UNSAFE_TELEPORTS).add(
                Blocks.LAVA,
                Blocks.LAVA_CAULDRON,
                Blocks.MAGMA_BLOCK,
                Blocks.CACTUS
        ).addTags(
                BlockTags.CAMPFIRES,
                BlockTags.FIRE
        );
        tag(ModBlockTags.TRAVERSAL_TOWER_FLOORS).add(
                Blocks.BIRCH_PLANKS,
                Blocks.DEEPSLATE_TILES,
                Blocks.CRACKED_DEEPSLATE_TILES,
                Blocks.DEEPSLATE_BRICKS,
                Blocks.CRACKED_DEEPSLATE_BRICKS
        );
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.WHISPERING_ANCHOR.get());
        tag(BlockTags.DRAGON_IMMUNE).add(ModBlocks.WHISPERING_ANCHOR.get());
        tag(Tags.Blocks.RELOCATION_NOT_SUPPORTED).add(ModBlocks.WHISPERING_ANCHOR.get());
    }
}
