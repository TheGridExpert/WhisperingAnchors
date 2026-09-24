package net.grid.whisperinganchors.data.provider;

import net.grid.whisperinganchors.data.provider.tag.ModBannerPatternTagsProvider;
import net.grid.whisperinganchors.data.provider.tag.ModBiomeTagsProvider;
import net.grid.whisperinganchors.data.provider.tag.ModBlockTagsProvider;
import net.grid.whisperinganchors.data.provider.tag.ModDamageTypeTagsProvider;
import net.grid.whisperinganchors.data.provider.tag.ModEntityTypeTagsProvider;
import net.grid.whisperinganchors.data.provider.tag.ModItemTagsProvider;
import net.grid.whisperinganchors.data.provider.tag.ModStructureTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ModTagsProvider {

    public static void register(DataGenerator generator, PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProviderFuture, ExistingFileHelper existingFileHelper) {
        ModBlockTagsProvider blockTagsProvider = new ModBlockTagsProvider(output, lookupProviderFuture, existingFileHelper);
        generator.addProvider(true, blockTagsProvider);
        generator.addProvider(true, new ModItemTagsProvider(output, lookupProviderFuture, blockTagsProvider.contentsGetter(), existingFileHelper));
        generator.addProvider(true, new ModEntityTypeTagsProvider(output, lookupProviderFuture, existingFileHelper));
        generator.addProvider(true, new ModDamageTypeTagsProvider(output, lookupProviderFuture, existingFileHelper));
        generator.addProvider(true, new ModBannerPatternTagsProvider(output, lookupProviderFuture, existingFileHelper));
        generator.addProvider(true, new ModBiomeTagsProvider(output, lookupProviderFuture, existingFileHelper));
        generator.addProvider(true, new ModStructureTagsProvider(output, lookupProviderFuture, existingFileHelper));
    }
}
