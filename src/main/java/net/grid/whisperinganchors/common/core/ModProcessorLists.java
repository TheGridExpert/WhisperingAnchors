package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.world.structure.processor.RadialFadeProcessor;
import net.grid.whisperinganchors.common.world.structure.processor.WeatheringProcessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.CappedProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosAlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.AppendLoot;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

public class ModProcessorLists {

    public static final ResourceKey<StructureProcessorList> TRAVERSAL_TOWER = create("traversal_tower");
    public static final ResourceKey<StructureProcessorList> TRAVERSAL_TOWER_UNDERGROUND = create("traversal_tower_underground");

    public static void bootstrap(BootstrapContext<StructureProcessorList> context) {
        context.register(TRAVERSAL_TOWER, new StructureProcessorList(List.of(
                weathering(),
                new CappedProcessor(buriedLoot(ModLootTables.TRAVERSAL_TOWER_ARCHAEOLOGY_GENERAL), UniformInt.of(5, 7))
        )));

        context.register(TRAVERSAL_TOWER_UNDERGROUND, new StructureProcessorList(List.of(
                new RadialFadeProcessor(new BlockMatchTest(Blocks.GRAVEL), 5, 5, 4.0F, 7.0F),
                weathering(),
                new CappedProcessor(buriedLoot(ModLootTables.TRAVERSAL_TOWER_ARCHAEOLOGY_GUARANTEED), ConstantInt.of(1)),
                new CappedProcessor(buriedLoot(ModLootTables.TRAVERSAL_TOWER_ARCHAEOLOGY_GENERAL), UniformInt.of(24, 48))
        )));
    }

    private static StructureProcessor weathering() {
        return new WeatheringProcessor(List.of(
                new WeatheringProcessor.Rule(Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS, 0.24F),
                new WeatheringProcessor.Rule(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, 0.24F),
                new WeatheringProcessor.Rule(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, 0.08F),
                new WeatheringProcessor.Rule(Blocks.COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_STAIRS, 0.08F),
                new WeatheringProcessor.Rule(Blocks.COBBLESTONE_WALL, Blocks.MOSSY_COBBLESTONE_WALL, 0.08F)
        ));
    }

    private static RuleProcessor buriedLoot(ResourceKey<LootTable> lootTable) {
        return new RuleProcessor(List.of(new ProcessorRule(new BlockMatchTest(Blocks.GRAVEL), AlwaysTrueTest.INSTANCE, PosAlwaysTrueTest.INSTANCE, Blocks.SUSPICIOUS_GRAVEL.defaultBlockState(), new AppendLoot(lootTable))));
    }

    private static ResourceKey<StructureProcessorList> create(String name) {
        return ResourceKey.create(Registries.PROCESSOR_LIST, WhisperingAnchors.modLoc(name));
    }
}
