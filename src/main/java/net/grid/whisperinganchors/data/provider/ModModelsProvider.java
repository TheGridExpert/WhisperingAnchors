package net.grid.whisperinganchors.data.provider;

import net.grid.whisperinganchors.data.provider.model.ModBlockStateProvider;
import net.grid.whisperinganchors.data.provider.model.ModItemModelProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModModelsProvider {

    public static void register(DataGenerator generator, PackOutput output, ExistingFileHelper existingFileHelper, boolean includeClient) {
        generator.addProvider(includeClient, new ModBlockStateProvider(output, existingFileHelper));
        generator.addProvider(includeClient, new ModItemModelProvider(output, existingFileHelper));
    }
}
