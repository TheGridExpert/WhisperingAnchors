package net.grid.whisperinganchors.common.core;

import com.mojang.datafixers.util.Pair;
import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.List;

public class ModTemplatePools {

    public static final ResourceKey<StructureTemplatePool> TRAVERSAL_TOWER_BASE_PLATES = create("traversal_tower/base_plates");
    public static final ResourceKey<StructureTemplatePool> TRAVERSAL_TOWER_TOWERS = create("traversal_tower/towers");
    public static final ResourceKey<StructureTemplatePool> TRAVERSAL_TOWER_UNDERGROUND = create("traversal_tower/underground");
    public static final ResourceKey<StructureTemplatePool> TRAVERSAL_TOWER_FEATURE_PLATES = create("traversal_tower/feature_plates");
    public static final ResourceKey<StructureTemplatePool> TRAVERSAL_TOWER_FEATURES = create("traversal_tower/features");
    public static final ResourceKey<StructureTemplatePool> TRAVERSAL_TOWER_FOOD_PLATES = create("traversal_tower/food_plates");
    public static final ResourceKey<StructureTemplatePool> TRAVERSAL_TOWER_STOVES = create("traversal_tower/stoves");
    public static final ResourceKey<StructureTemplatePool> TRAVERSAL_TOWER_BENCHES = create("traversal_tower/benches");

    public static final ResourceKey<StructureTemplatePool> TRAVERSAL_TOWER_WEST_PLATE_SLOT = create("traversal_tower/plate_slots/west");
    public static final ResourceKey<StructureTemplatePool> TRAVERSAL_TOWER_SOUTH_PLATE_SLOT = create("traversal_tower/plate_slots/south");
    public static final ResourceKey<StructureTemplatePool> TRAVERSAL_TOWER_EAST_PLATE_SLOT = create("traversal_tower/plate_slots/east");

    public static void bootstrap(BootstrapContext<StructureTemplatePool> context) {
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        HolderGetter<StructureProcessorList> processors = context.lookup(Registries.PROCESSOR_LIST);

        context.register(TRAVERSAL_TOWER_BASE_PLATES, new StructureTemplatePool(pools.getOrThrow(Pools.EMPTY), List.of(
                Pair.of(StructurePoolElement.legacy(getTraversalTowerPiece("base_plate")), 1)
        ), StructureTemplatePool.Projection.TERRAIN_MATCHING));

        context.register(TRAVERSAL_TOWER_TOWERS, new StructureTemplatePool(pools.getOrThrow(Pools.EMPTY), List.of(
                Pair.of(StructurePoolElement.single(getTraversalTowerPiece("tower"), processors.getOrThrow(ModProcessorLists.TRAVERSAL_TOWER)), 1)
        ), StructureTemplatePool.Projection.RIGID));

        context.register(TRAVERSAL_TOWER_UNDERGROUND, new StructureTemplatePool(pools.getOrThrow(Pools.EMPTY), List.of(
                Pair.of(StructurePoolElement.single(getTraversalTowerPiece("underground"), processors.getOrThrow(ModProcessorLists.TRAVERSAL_TOWER_UNDERGROUND)), 1)
        ), StructureTemplatePool.Projection.RIGID));

        context.register(TRAVERSAL_TOWER_FEATURE_PLATES, new StructureTemplatePool(pools.getOrThrow(Pools.EMPTY), List.of(
                Pair.of(StructurePoolElement.legacy(getTraversalTowerPiece("feature_plate")), 1)
        ), StructureTemplatePool.Projection.TERRAIN_MATCHING));

        context.register(TRAVERSAL_TOWER_FEATURES, new StructureTemplatePool(pools.getOrThrow(Pools.EMPTY), List.of(
                Pair.of(StructurePoolElement.legacy(getTraversalTowerPiece("feature_cage_with_allays")), 1),
                Pair.of(StructurePoolElement.legacy(getTraversalTowerPiece("feature_logs")), 2),
                Pair.of(StructurePoolElement.legacy(getTraversalTowerPiece("feature_basalt")), 2),
                Pair.of(StructurePoolElement.legacy(getTraversalTowerPiece("feature_deepslate")), 2),
                Pair.of(StructurePoolElement.legacy(getTraversalTowerPiece("feature_tent1")), 2),
                Pair.of(StructurePoolElement.legacy(getTraversalTowerPiece("feature_tent2")), 2),
                Pair.of(StructurePoolElement.legacy(getTraversalTowerPiece("feature_tent3")), 2),
                Pair.of(StructurePoolElement.empty(), 17)
        ), StructureTemplatePool.Projection.RIGID));

        context.register(TRAVERSAL_TOWER_FOOD_PLATES, new StructureTemplatePool(pools.getOrThrow(Pools.EMPTY), List.of(
                Pair.of(StructurePoolElement.legacy(getTraversalTowerPiece("food_plate")), 1)
        ), StructureTemplatePool.Projection.TERRAIN_MATCHING));

        context.register(TRAVERSAL_TOWER_STOVES, new StructureTemplatePool(pools.getOrThrow(Pools.EMPTY), List.of(
                Pair.of(StructurePoolElement.legacy(getTraversalTowerPiece("feature_stove"), processors.getOrThrow(ModProcessorLists.TRAVERSAL_TOWER_FEATURES)), 1)
        ), StructureTemplatePool.Projection.RIGID));

        context.register(TRAVERSAL_TOWER_BENCHES, new StructureTemplatePool(pools.getOrThrow(Pools.EMPTY), List.of(
                Pair.of(StructurePoolElement.legacy(getTraversalTowerPiece("feature_bench")), 2),
                Pair.of(StructurePoolElement.empty(), 1)
        ), StructureTemplatePool.Projection.RIGID));
    }

    private static String getTraversalTowerPiece(String name) {
        return WhisperingAnchors.modLoc("traversal_tower/" + name).toString();
    }

    private static ResourceKey<StructureTemplatePool> create(String name) {
        return ResourceKey.create(Registries.TEMPLATE_POOL, WhisperingAnchors.modLoc(name));
    }
}
