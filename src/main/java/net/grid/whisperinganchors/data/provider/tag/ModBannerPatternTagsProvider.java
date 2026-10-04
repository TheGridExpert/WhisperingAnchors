package net.grid.whisperinganchors.data.provider.tag;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModBannerPatterns;
import net.grid.whisperinganchors.common.core.tag.ModBannerPatternTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BannerPatternTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBannerPatternTagsProvider extends BannerPatternTagsProvider {

    public ModBannerPatternTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, provider, WhisperingAnchors.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModBannerPatternTags.PATTERN_ITEM_ANCHOR).add(ModBannerPatterns.ANCHOR);
    }
}
