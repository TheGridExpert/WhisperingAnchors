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

    public static final ResourceKey<StructureTemplatePool> TRAVERSAL_TOWER = create("traversal_tower");
    public static final ResourceKey<StructureTemplatePool> TRAVERSAL_TOWER_UNDERGROUND = create("traversal_tower_underground");

    public static void bootstrap(BootstrapContext<StructureTemplatePool> context) {
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        HolderGetter<StructureProcessorList> processors = context.lookup(Registries.PROCESSOR_LIST);

        context.register(TRAVERSAL_TOWER, new StructureTemplatePool(pools.getOrThrow(Pools.EMPTY), List.of(
                Pair.of(StructurePoolElement.single(WhisperingAnchors.modLoc("traversal_tower").toString(), processors.getOrThrow(ModProcessorLists.TRAVERSAL_TOWER)), 1)
        ), StructureTemplatePool.Projection.RIGID));

        context.register(TRAVERSAL_TOWER_UNDERGROUND, new StructureTemplatePool(pools.getOrThrow(Pools.EMPTY), List.of(
                Pair.of(StructurePoolElement.single(WhisperingAnchors.modLoc("traversal_tower_underground").toString(), processors.getOrThrow(ModProcessorLists.TRAVERSAL_TOWER_UNDERGROUND)), 1)
        ), StructureTemplatePool.Projection.RIGID));
    }

    private static ResourceKey<StructureTemplatePool> create(String name) {
        return ResourceKey.create(Registries.TEMPLATE_POOL, WhisperingAnchors.modLoc(name));
    }
}
