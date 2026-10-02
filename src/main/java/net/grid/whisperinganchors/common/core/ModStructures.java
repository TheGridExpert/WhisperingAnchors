package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.tag.ModBiomeTags;
import net.grid.whisperinganchors.common.core.tag.ModBlockTags;
import net.grid.whisperinganchors.common.world.structure.TraversalTowerStructure;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class ModStructures {

    public static final ResourceKey<Structure> TRAVERSAL_TOWER = create("traversal_tower");

    public static void bootstrap(BootstrapContext<Structure> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);

        context.register(TRAVERSAL_TOWER, new TraversalTowerStructure(
                new Structure.StructureSettings.Builder(biomes.getOrThrow(ModBiomeTags.HAS_TRAVERSAL_TOWER))
                        .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
                        .terrainAdapation(TerrainAdjustment.BEARD_THIN)
                        .build(),
                pools.getOrThrow(ModTemplatePools.TRAVERSAL_TOWER),
                0,
                -55, -15, 10, 13, 10, 2, 8,
                UniformInt.of(5, 8), UniformInt.of(0, 2), 0.5F, ModBlockTags.TRAVERSAL_TOWER_FLOORS
        ));
    }

    private static ResourceKey<Structure> create(String name) {
        return ResourceKey.create(Registries.STRUCTURE, WhisperingAnchors.modLoc(name));
    }
}
