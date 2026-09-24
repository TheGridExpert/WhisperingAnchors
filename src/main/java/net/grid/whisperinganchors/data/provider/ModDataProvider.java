package net.grid.whisperinganchors.data.provider;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModBannerPatters;
import net.grid.whisperinganchors.common.core.ModDamageTypes;
import net.grid.whisperinganchors.common.core.ModProcessorLists;
import net.grid.whisperinganchors.common.core.ModStructureSets;
import net.grid.whisperinganchors.common.core.ModStructures;
import net.grid.whisperinganchors.common.core.ModTemplatePools;
import net.grid.whisperinganchors.data.provider.loot.ModArchaeologyLootProvider;
import net.grid.whisperinganchors.data.provider.loot.ModBlockLootProvider;
import net.grid.whisperinganchors.data.provider.loot.ModChestLootProvider;
import net.grid.whisperinganchors.data.provider.loot.ModGlobalLootModifierProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = WhisperingAnchors.MODID)
public class ModDataProvider {

    private static final RegistrySetBuilder DATA_BUILDER = new RegistrySetBuilder()
            .add(Registries.DAMAGE_TYPE, ModDamageTypes::bootstrap)
            .add(Registries.BANNER_PATTERN, ModBannerPatters::bootstrap)
            .add(Registries.PROCESSOR_LIST, ModProcessorLists::bootstrap)
            .add(Registries.TEMPLATE_POOL, ModTemplatePools::bootstrap)
            .add(Registries.STRUCTURE, ModStructures::bootstrap)
            .add(Registries.STRUCTURE_SET, ModStructureSets::bootstrap);

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        DatapackBuiltinEntriesProvider provider = new DatapackBuiltinEntriesProvider(packOutput, lookupProvider, DATA_BUILDER, Set.of(WhisperingAnchors.MODID));
        lookupProvider = provider.getRegistryProvider();
        generator.addProvider(true, provider);

        ModTagsProvider.register(generator, packOutput, lookupProvider, event.getExistingFileHelper());
        ModModelsProvider.register(generator, packOutput, event.getExistingFileHelper(), event.includeClient());
        generator.addProvider(event.includeClient(), new ModSoundProvider(packOutput, event.getExistingFileHelper()));
        generator.addProvider(true, new ModGlobalLootModifierProvider(packOutput, lookupProvider));
        generator.addProvider(true, new LootTableProvider(packOutput, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(registries -> new ModArchaeologyLootProvider(), LootContextParamSets.ARCHAEOLOGY),
                new LootTableProvider.SubProviderEntry(registries -> new ModChestLootProvider(), LootContextParamSets.CHEST)
        ), lookupProvider));
    }
}
